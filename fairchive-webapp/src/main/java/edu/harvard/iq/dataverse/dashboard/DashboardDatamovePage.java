package edu.harvard.iq.dataverse.dashboard;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.logging.Level.WARNING;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.NavigationWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.move.AdditionalMoveStatus;
import edu.harvard.iq.dataverse.engine.command.exception.move.MoveException;
import edu.harvard.iq.dataverse.engine.command.impl.MoveDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.MoveDataverseCommand;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;
import edu.harvard.iq.dataverse.util.UIMessages;

@SuppressWarnings("serial")
@ViewScoped
@Named("DashboardDatamovePage")
public class DashboardDatamovePage implements Serializable {
    private static final Logger logger = Logger
            .getLogger(DashboardDatamovePage.class.getCanonicalName());

    private final DataverseRequestServiceBean requestService;
    private final DatasetService datasetService;
    private final DataverseRepository dataverseRepo;
    private final DataverseSession session;
    private final NavigationWrapper navigation;
    private final EjbDataverseEngine commandEngine;
    private final SettingsWrapper settings;
    private final UIMessages uiMessages;

    private boolean forceMove = false;

    private List<Dataset> sourceDatasets = new ArrayList<>();
    private Dataverse targetDataverse;

    private Dataverse sourceDataverse;

    @Inject
    public DashboardDatamovePage(final DataverseRequestServiceBean requestService,
            final DatasetService datasetService,
            final DataverseRepository dataverseRepo,
            final DataverseSession session,
            final NavigationWrapper navigation,
            final EjbDataverseEngine commandEngine,
            final SettingsWrapper settings,
            final UIMessages uiMessages) {
        this.requestService = requestService;
        this.datasetService = datasetService;
        this.dataverseRepo = dataverseRepo;
        this.session = session;
        this.navigation = navigation;
        this.commandEngine = commandEngine;
        this.settings = settings;
        this.uiMessages = uiMessages;
    }

    public boolean isForceMove() {
        return forceMove;
    }

    public List<Dataset> getSourceDatasets() {
        return sourceDatasets;
    }

    public Dataverse getTargetDataverse() {
        return targetDataverse;
    }

    public Dataverse getSourceDataverse() {
        return sourceDataverse;
    }

    // -------------------- LOGIC --------------------
    public String verifyAccess() {
        return this.session.canEditDashboard() 
                ? EMPTY
                : this.navigation.notAuthorized();
    }

    public List<Dataset> completeSourceDataset(final String query) {
        if (query.contains("/")) {
            final Dataset ds = this.datasetService.findByGlobalId(query);
            return ds != null ? singletonList(ds) : emptyList();
        } else {
            return emptyList();
        }
    }

    public List<Dataverse> completeDataverse(final String query) {
        return this.dataverseRepo.findByAliasOrNameOrAffiliation(query, query,
                query);
    }

    public void moveDataset() {
        if (sourceDatasets == null || sourceDatasets.isEmpty()
                || targetDataverse == null) {
            // We should never get here, but in case of some unexpected failure we
            // should be prepared nevertheless
            this.uiMessages.addErrorMessage(
                    getStringFromBundle("dashboard.datamove.empty.fields"));
            return;
        }

        List<String> successfulIds = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();
        for (Dataset source : sourceDatasets) {
            Summary summary = new Summary("dashboard.datamove.dataset", this.uiMessages);
            try {
                summary.addParameter(source.getDisplayName())
                        .addParameter(extractSourcePersistentId(source))
                        .addParameter(targetDataverse.getDisplayName());

                DataverseRequest dataverseRequest = requestService
                        .getDataverseRequest();
                String previousSourceAlias = extractSourceAlias(source);
                commandEngine.submit(new MoveDatasetCommand(dataverseRequest, source,
                        targetDataverse, forceMove));
                logger.info(createMessageWithDatasetMoveInfo(source, "Moved",
                        previousSourceAlias));
                successfulIds.add(extractSourcePersistentId(source));
            } catch (MoveException me) {
                logger.log(WARNING, createMessageWithDatasetMoveInfo(source, "Unable to move"), me);
                summary.addParameter(me)
                        .addParameter(createForceInfoIfApplicable(me));
                failureMessages.add(summary.getFailureMessageDetail());
            } catch (CommandException ce) {
                logger.log(WARNING, createMessageWithDatasetMoveInfo(source, "Unable to move"), ce);
                summary.addParameter(getStringFromBundle(
                        "dashboard.datamove.dataset.message.failure.summary"));
                failureMessages.add(summary.getFailureMessageDetail());
            }
        }

        resetDatasetMoveFields();
        showDatasetsMovedMessage(successfulIds, failureMessages,
                targetDataverse.getDisplayName());
    }

    public void moveDataverse() {
        if (sourceDataverse == null || targetDataverse == null) {
            this.uiMessages.addErrorMessage(
                    getStringFromBundle("dashboard.datamove.empty.fields"));
            return;
        }

        Summary summary = new Summary("dashboard.datamove.dataverse", this.uiMessages)
                .addParameter(extractDataverseAlias(sourceDataverse))
                .addParameter(extractDataverseAlias(targetDataverse));

        try {
            DataverseRequest dataverseRequest = requestService.getDataverseRequest();
            commandEngine.submit(new MoveDataverseCommand(dataverseRequest,
                    sourceDataverse, targetDataverse, forceMove));
            logger.info(createMessageWithDataverseMoveInfo("Moved"));
            resetDataverseMoveFields();
            summary.showSuccessMessage();
        } catch (MoveException me) {
            logger.log(WARNING, createMessageWithDataverseMoveInfo("Unable to move"), me);
            summary.addParameter(me)
                    .addParameter(createForceInfoIfApplicable(me))
                    .showFailureMessage();
        } catch (CommandException ce) {
            logger.log(WARNING, createMessageWithDataverseMoveInfo("Unable to move"), ce);
            this.uiMessages.addErrorMessage(
                    getStringFromBundle("dashboard.datamove.dataverse.message.failure.summary"), EMPTY);
        }
    }

    public String getMessageDetails() {
        return getStringFromBundle("dashboard.datamove.message.details",
                settings.getGuidesBaseUrl(), settings.getGuidesVersion());
    }

    // -------------------- PRIVATE --------------------

    private void showDatasetsMovedMessage(List<String> successfulIds,
            List<String> failureMessages, String dataverseName) {
        StringBuilder sb = new StringBuilder();

        if (!successfulIds.isEmpty()) {
            sb.append(getStringFromBundle(
                    "dashboard.datamove.dataset.message.success.multiple",
                    StringUtils.join(successfulIds, ", "), dataverseName));
        }

        if (!failureMessages.isEmpty()) {
            if (!successfulIds.isEmpty()) {
                sb.append("<hr>");
            }
            sb.append(String.join("<hr>", failureMessages));
        }

        this.uiMessages.addInfoMessage(sb.toString().replaceAll("<br><br>", "<br>"));
    }

    private static String extractSourcePersistentId(Dataset source) {
        return Optional.ofNullable(source)
                .map(Dataset::getGlobalId)
                .map(GlobalId::asString)
                .orElse(EMPTY);
    }

    private static String extractSourceAlias(Dataset source) {
        return Optional.ofNullable(source)
                .map(Dataset::getOwner)
                .map(Dataverse::getAlias)
                .orElse(EMPTY);
    }

    private static String extractDataverseAlias(Dataverse dataverse) {
        return Optional.ofNullable(dataverse)
                .map(Dataverse::getAlias)
                .orElse(EMPTY);
    }

    private String createMessageWithDatasetMoveInfo(Dataset sourceDs, String message,
            String source) {
        return String.format("%s %s from %s to %s",
                message, extractSourcePersistentId(sourceDs), source,
                extractDataverseAlias(targetDataverse));
    }

    private String createMessageWithDatasetMoveInfo(Dataset sourceDs,
            String message) {
        return createMessageWithDatasetMoveInfo(sourceDs, message,
                extractSourceAlias(sourceDs));
    }

    private String createMessageWithDataverseMoveInfo(String message) {
        return String.format("%s %s to %s",
                message, extractDataverseAlias(sourceDataverse),
                extractDataverseAlias(targetDataverse));
    }

    private String createForceInfoIfApplicable(MoveException mde) {
        return isForcingPossible(mde)
                ? getStringFromBundle("dashboard.datamove.command.suggestForce",
                        settings.getGuidesBaseUrl(), settings.getGuidesVersion())
                : EMPTY;
    }

    private boolean isForcingPossible(MoveException mde) {
        return mde.getDetails().stream()
                .allMatch(AdditionalMoveStatus::isPassByForcePossible);
    }
    
    private void resetDatasetMoveFields() {
        this.sourceDatasets = new ArrayList<>();
        this.targetDataverse = null;
    }

    private void resetDataverseMoveFields() {
        this.sourceDataverse = null;
        this.targetDataverse = null;
    }

    // -------------------- SETTERS --------------------

    public void setForceMove(final boolean forceMove) {
        this.forceMove = forceMove;
    }

    public void setSourceDatasets(final List<Dataset> sourceDatasets) {
        this.sourceDatasets = sourceDatasets;
    }

    public void setTargetDataverse(final Dataverse targetDataverse) {
        this.targetDataverse = targetDataverse;
    }

    public void setSourceDataverse(final Dataverse sourceDataverse) {
        this.sourceDataverse = sourceDataverse;
    }

    // -------------------- INNER CLASSES ---------------------

    private static class Summary {

        private final String key;
        private final List<String> summaryParameters = new ArrayList<>();
        private final UIMessages uiMessages;

        public Summary(final String key, final UIMessages uiMessages) {
            this.key = key;
            this.uiMessages = uiMessages;
        }

        public Summary addParameter(final String param) {
            this.summaryParameters.add(param != null ? param : EMPTY);
            return this;
        }

        public Summary addParameter(final MoveException mde) {
            this.summaryParameters.add(mde.getDetails().stream()
                    .map(AdditionalMoveStatus::getMessageKey)
                    .map(BundleUtil::getStringFromBundle)
                    .collect(joining(" ")));
            return this;
        }

        public void showSuccessMessage() {
            this.uiMessages.addFlashSuccessMessage(getStringFromBundle(
                    this.key.concat(".message.success"), this.summaryParameters.toArray()));
        }

        public String getFailureMessageDetail() {
            return getStringFromBundle(this.key.concat(".message.failure.details"),
                    this.summaryParameters.toArray());
        }

        public void showFailureMessage() {
            this.uiMessages.addErrorMessage(
                    getStringFromBundle(this.key.concat(".message.failure.summary")),
                    getFailureMessageDetail());
        }
    }
}
