package edu.harvard.iq.dataverse.harvest.server.web.servlet;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.OAIServerEnabled;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SystemEmail;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.dspace.xoai.dataprovider.model.MetadataFormat.metadataFormat;
import static org.dspace.xoai.model.oaipmh.Verb.Type.GetRecord;
import static org.dspace.xoai.model.oaipmh.Verb.Type.ListRecords;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;

import org.dspace.xoai.dataprovider.builder.OAIRequestParametersBuilder;
import org.dspace.xoai.dataprovider.exceptions.OAIException;
import org.dspace.xoai.dataprovider.model.Context;
import org.dspace.xoai.dataprovider.model.MetadataFormat;
import org.dspace.xoai.dataprovider.repository.ItemRepository;
import org.dspace.xoai.dataprovider.repository.Repository;
import org.dspace.xoai.dataprovider.repository.RepositoryConfiguration;
import org.dspace.xoai.dataprovider.repository.SetRepository;
import org.dspace.xoai.model.oaipmh.DeletedRecord;
import org.dspace.xoai.model.oaipmh.Granularity;
import org.dspace.xoai.model.oaipmh.OAIPMH;
import org.dspace.xoai.model.oaipmh.Verb;
import org.dspace.xoai.services.impl.SimpleResumptionTokenFormat;
import org.dspace.xoai.services.impl.UTCDateProvider;
import org.dspace.xoai.xml.XmlWriter;

import com.lyncode.xml.exceptions.XmlWriteException;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.harvest.server.OAIRecordServiceBean;
import edu.harvard.iq.dataverse.harvest.server.OAISetServiceBean;
import edu.harvard.iq.dataverse.harvest.server.xoai.XdataProvider;
import edu.harvard.iq.dataverse.harvest.server.xoai.XgetRecord;
import edu.harvard.iq.dataverse.harvest.server.xoai.XitemRepository;
import edu.harvard.iq.dataverse.harvest.server.xoai.XlistRecords;
import edu.harvard.iq.dataverse.harvest.server.xoai.XsetRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import static org.dspace.xoai.xml.XmlWriter.defaultContext;

/**
 * @author Leonid Andreev Dedicated servlet for handling OAI-PMH requests. Uses
 *         lyncode XOAI data provider implementation for serving content. The
 *         servlet itself is somewhat influenced by the older OCLC OAIcat
 *         implementation.
 */
@SuppressWarnings("serial")
public class OAIServlet extends HttpServlet {
	private static final Logger logger = Logger
			.getLogger("edu.harvard.iq.dataverse.harvest.server.web.servlet.OAIServlet");

	private static final String OAI_PMH = "OAI-PMH";
	private static final byte[] RESPONSEDATE_FIELD = "responseDate".getBytes();
	private static final String DATAVERSE_EXTENDED_METADATA_FORMAT = "dataverse_json";
	private static final String DATAVERSE_EXTENDED_METADATA_INFO = "Custom Dataverse metadata in JSON format (Dataverse4 to Dataverse4 harvesting only)";
	private static final String DATAVERSE_EXTENDED_METADATA_SCHEMA = "JSON schema pending";

	private static final byte LT = '<';
	private static final byte GT = '>';
	private static final byte SLASH = '/';
	private static final byte[] OAI_PMH_BYTES = OAI_PMH.getBytes();
	private static final byte[] OAIPMH_HEADER = "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">"
			.getBytes();

	private OAISetServiceBean setService;
	private OAIRecordServiceBean recordService;
	private SettingsServiceBean settingsService;
	private DataverseDao dataverseDao;
	private SystemConfig systemConfig;
	private ExportService exportService;
	private DatasetVersionRepository datasetVersionRepository;

	private Context xoaiContext;
	private SetRepository setRepository;
	private ItemRepository itemRepository;
	private RepositoryConfiguration repositoryConfiguration;
	private Repository xoaiRepository;
	private XdataProvider dataProvider;

	private final static UTCDateProvider dateProvider = new UTCDateProvider();

	// -------------------- CONSTRUCTORS --------------------

	public OAIServlet() {
	}

	@Inject
	public OAIServlet(OAISetServiceBean setService, OAIRecordServiceBean recordService,
			SettingsServiceBean settingsService, DataverseDao dataverseDao, SystemConfig systemConfig,
			ExportService exportService, final DatasetVersionRepository datasetVersionRepository) {
		this.setService = setService;
		this.recordService = recordService;
		this.settingsService = settingsService;
		this.dataverseDao = dataverseDao;
		this.systemConfig = systemConfig;
		this.exportService = exportService;
		this.datasetVersionRepository = datasetVersionRepository;
	}

	// -------------------- GETTERS --------------------

	protected Context getXoaiContext() {
		return xoaiContext;
	}

	protected Repository getXoaiRepository() {
		return xoaiRepository;
	}

	/**
	 * Returns a short description of the servlet.
	 */
	@Override
	public String getServletInfo() {
		return "Dataverse OAI Servlet";
	}

	// -------------------- LOGIC --------------------

	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		xoaiContext = createContext();

		if (isDataverseOaiExtensionsSupported()) {
			xoaiContext = addDataverseJsonMetadataFormat(xoaiContext);
		}

		setRepository = new XsetRepository(setService);
		itemRepository = new XitemRepository(recordService, systemConfig);

		repositoryConfiguration = createRepositoryConfiguration();

		xoaiRepository = new Repository().withSetRepository(setRepository).withItemRepository(itemRepository)
				.withResumptionTokenFormatter(new SimpleResumptionTokenFormat())
				.withConfiguration(repositoryConfiguration);

		dataProvider = new XdataProvider(getXoaiContext(), getXoaiRepository());
	}

	/**
	 * Handles the HTTP <code>POST</code> method.
	 *
	 * @param request  servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException      if an I/O error occurs
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Handles the HTTP <code>GET</code> method.
	 *
	 * @param request  servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException      if an I/O error occurs
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	protected OAIRequestParametersBuilder newXoaiRequest() {
		return new OAIRequestParametersBuilder();
	}

	public boolean isHarvestingServerEnabled() {
		return settingsService.isTrueForKey(OAIServerEnabled);
	}

	// -------------------- PRIVATE --------------------

	private Context createContext() {
		Context context = new Context();
		addSupportedMetadataFormats(context);
		return context;
	}

	private void addSupportedMetadataFormats(Context context) {
		for (final Exporter exporter : exportService.exporters()) {
			if (!exporter.isXMLFormat() || !exporter.isHarvestable()) {
				continue;
			}
			MetadataFormat metadataFormat = metadataFormat(exporter.getProviderName());
			if (exporter.getXMLNameSpace().isEmpty() && exporter.getXMLSchemaLocation().isEmpty()) {
				continue;
			}
			metadataFormat.withNamespace(exporter.getXMLNameSpace());
			metadataFormat.withSchemaLocation(exporter.getXMLSchemaLocation());
			context.withMetadataFormat(metadataFormat);
		}
	}

	private Context addDataverseJsonMetadataFormat(Context context) {
		MetadataFormat metadataFormat = MetadataFormat.metadataFormat(DATAVERSE_EXTENDED_METADATA_FORMAT);
		metadataFormat.withNamespace(DATAVERSE_EXTENDED_METADATA_INFO);
		metadataFormat.withSchemaLocation(DATAVERSE_EXTENDED_METADATA_SCHEMA);
		context.withMetadataFormat(metadataFormat);
		return context;
	}

	private boolean isDataverseOaiExtensionsSupported() {
		return true;
	}

	private RepositoryConfiguration createRepositoryConfiguration() {
		// TODO:
		// some of the settings below - such as the max list numbers -
		// need to be configurable!

		String dataverseName = dataverseDao.findRootDataverse().getName();
		String repositoryName = isEmpty(dataverseName) || "Root".equals(dataverseName) ? "Test Dataverse OAI Archive"
				: dataverseName + " Dataverse OAI Archive";
		Optional<Date> earliestDate = recordService.findEarliestDate();

		RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration()
				.withRepositoryName(repositoryName).withBaseUrl(systemConfig.getDataverseSiteUrl() + "/oai")
				.withCompression("gzip") // ?
				.withCompression("deflate") // ?
				.withAdminEmail(settingsService.getValueForKey(SystemEmail)).withDeleteMethod(DeletedRecord.TRANSIENT)
				.withGranularity(Granularity.Second).withMaxListIdentifiers(100).withMaxListRecords(100)
				.withMaxListSets(100).withEarliestDate(earliestDate.orElse(new Date()))
				.withDescription(createDescription());

		return repositoryConfiguration;
	}

	private String createDescription() {
		return "<description>" + "<oai-identifier xmlns=\"http://www.openarchives.org/OAI/2.0/oai-identifier\">"
				+ "<scheme>oai</scheme>" + "<repositoryIdentifier>" + this.systemConfig.getDataverseServer()
				+ "</repositoryIdentifier>" + "<delimiter>:</delimiter>" + "<sampleIdentifier>oai:"
				+ this.systemConfig.getDataverseServer() + ":doi%3A10.18150%2FAB1CD2</sampleIdentifier>"
				+ "</oai-identifier>" + "</description>";
	}

	private void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		try {
			if (!isHarvestingServerEnabled()) {
				response.sendError(SC_SERVICE_UNAVAILABLE, "Sorry. OAI Service is disabled on this Dataverse node.");
				return;
			}

			OAIRequestParametersBuilder parametersBuilder = newXoaiRequest();

			for (Object p : request.getParameterMap().keySet()) {
				String parameterName = (String) p;
				String parameterValue = request.getParameter(parameterName);
				parametersBuilder = parametersBuilder.with(parameterName, parameterValue);
			}

			OAIPMH handle = dataProvider.handle(parametersBuilder);
			response.setContentType("text/xml;charset=UTF-8");

			if (isGetRecord(request) && !handle.hasErrors()) {
				writeGetRecord(response, handle);
			} else if (isListRecords(request) && !handle.hasErrors()) {
				writeListRecords(response, handle);
			} else {
				XmlWriter xmlWriter = new XmlWriter(response.getOutputStream());
				xmlWriter.write(handle);
				xmlWriter.flush();
				xmlWriter.close();
			}

		} catch (IOException ex) {
			logger.warning("IO exception in Get; " + ex.getMessage());
			throw new ServletException("IO Exception in Get", ex);
		} catch (OAIException oex) {
			logger.warning("OAI exception in Get; " + oex.getMessage());
			throw new ServletException("OAI Exception in Get", oex);
		} catch (XMLStreamException xse) {
			logger.warning("XML Stream exception in Get; " + xse.getMessage());
			throw new ServletException("XML Stream Exception in Get", xse);
		} catch (XmlWriteException xwe) {
			logger.warning("XML Write exception in Get; " + xwe.getMessage());
			throw new ServletException("XML Write Exception in Get", xwe);
		} catch (Exception e) {
			logger.warning("Unknown exception in Get; " + e.getMessage());
			throw new ServletException("Unknown servlet exception in Get.", e);
		}
	}

	// Custom methods for the potentially expensive GetRecord and ListRecords
	// requests:
	private void writeListRecords(HttpServletResponse response, OAIPMH handle) 
			throws IOException, XmlWriteException, XMLStreamException {
		final OutputStream out = response.getOutputStream();
		final Verb verb = handle.getVerb();

		writeHeader(out, handle);

		if (verb == null) {
			throw new IOException("An error or a valid response must be set");
		}

		if (!verb.getType().equals(ListRecords)) {
			throw new IOException("writeListRecords() called on a non-ListRecords verb");
		}

		out.write(LT);
		out.write(verb.getType().displayName().getBytes(UTF_8));
		out.write(GT);

		((XlistRecords) verb).writeToStream(out, exportService, datasetVersionRepository,
				systemConfig.getDataverseSiteUrl());

		writeFooter(out, verb);
	}

	private void writeGetRecord(HttpServletResponse response, OAIPMH handle)
			throws IOException, XmlWriteException, XMLStreamException {
		final OutputStream out = response.getOutputStream();
		final Verb verb = handle.getVerb();

		writeHeader(out, handle);

		if (verb == null) {
			throw new IOException("An error or a valid response must be set");
		}

		if (!verb.getType().equals(GetRecord)) {
			throw new IOException("writeListRecords() called on a non-GetRecord verb");
		}

		out.write(LT);
		out.write(verb.getType().displayName().getBytes(UTF_8));
		out.write(GT);

		((XgetRecord) verb).writeToStream(out, exportService, this.datasetVersionRepository,
				systemConfig.getDataverseSiteUrl());

		writeFooter(out, verb);
	}

	private static void writeHeader(final OutputStream out, final OAIPMH handle) 
			throws IOException, XMLStreamException, XmlWriteException {

		out.write(OAIPMH_HEADER);

		out.write(LT);
		out.write(RESPONSEDATE_FIELD);
		out.write(GT);
		out.write(dateProvider.format(handle.getResponseDate(), Granularity.Second).getBytes(UTF_8));
		out.write(LT);
		out.write(SLASH);
		out.write(RESPONSEDATE_FIELD);
		out.write(GT);

		final XmlWriter writer = new XmlWriter(out, defaultContext());
		writer.writeElement("request", handle.getRequest());
		writer.flush();
		// no close
	}

	private static boolean isGetRecord(final HttpServletRequest request) {
		return "GetRecord".equals(request.getParameter("verb"));
	}

	private static boolean isListRecords(final HttpServletRequest request) {
		return "ListRecords".equals(request.getParameter("verb"));
	}

	private static void writeFooter(final OutputStream out, final Verb verb) throws IOException {
		out.write(LT);
		out.write(SLASH);
		out.write(verb.getType().displayName().getBytes(UTF_8));
		out.write(GT);
		out.write(LT);
		out.write(SLASH);
		out.write(OAI_PMH_BYTES);
		out.write(GT);
	}
}
