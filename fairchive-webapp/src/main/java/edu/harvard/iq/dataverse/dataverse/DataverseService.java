package edu.harvard.iq.dataverse.dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.notification.NotificationObjectType.DATAVERSE;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CREATEDV;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.primefaces.model.DualListModel;

import com.google.common.collect.Lists;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.TempLogoStorageManager;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseThemeCommand;
import edu.harvard.iq.dataverse.error.DataverseError;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.UserNotificationService;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import io.vavr.control.Either;

@Stateless
public class DataverseService {

    private DataverseSession session;
    private DataverseRequestServiceBean requestService;
    private EjbDataverseEngine commandEngine;
    private UserNotificationService userNotificationService;
    private DataverseRepository repository;
    
    public DataverseService() {}
    
    @Inject
    public DataverseService(final DataverseSession session, 
    		final DataverseRequestServiceBean requestService,
			final EjbDataverseEngine commandEngine, 
			final UserNotificationService userNotificationService,
			final DataverseRepository repository) {

		this.session = session;
		this.requestService = requestService;
		this.commandEngine = commandEngine;
		this.userNotificationService = userNotificationService;
		this.repository = repository;
	}

	public Optional<Dataverse> findById(final Long id) {
    	return this.repository.findById(id);
    }
    
    public Optional<Dataverse> findByAlias(final String alias) {
    	return this.repository.findByAlias(alias);
    }
    
    public Either<DataverseError, Dataverse> saveNewDataverse(Collection<DataverseFieldTypeInputLevel> dftilToBeSaved,
                                                              Dataverse dataverse,
                                                              DualListModel<DatasetFieldType> facets) {

        if (!dataverse.isFacetRoot()) {
            facets.getTarget().clear();
        }

        if (session.isUserLoggedIn()) {
            Command<Dataverse> cmd = new CreateDataverseCommand(dataverse,
                                                                requestService.getDataverseRequest(),
                                                                facets.getTarget(),
                                                                Lists.newArrayList(dftilToBeSaved));

            try {
                dataverse = commandEngine.submit(cmd);
            } catch (CommandException ex) {
                return Either.left(new DataverseError(ex, getStringFromBundle("dataverse.create.failure")));
            }

            userNotificationService.sendNotificationWithEmail((AuthenticatedUser) session.getUser(), dataverse.getCreateDate(),
                                                              NotificationType.CREATEDV,
                                                              dataverse.getId(), NotificationObjectType.DATAVERSE);
        } else {
            return Either.left(new DataverseError(getStringFromBundle("dataverse.create.authenticatedUsersOnly")));
        }

        return Either.right(dataverse);
    }
	
	public Dataverse save(Dataverse dataverse, TempLogoStorageManager.Storage storage) {

		if (!this.session.isUserLoggedIn()) {
			throw new EJBException(
					getStringFromBundle("dataverse.create.authenticatedUsersOnly"));
		}
		final DataverseRequest request = this.requestService.getDataverseRequest();
		if (!dataverse.isThemeRoot()) {
			dataverse.getLatestVersion().setTheme(null);
			storage = null;
		}
		dataverse = this.commandEngine.submit(dataverse.isNew()
				? new CreateDataverseCommand(dataverse, request)
				: new UpdateDataverseCommand(dataverse, request));
        dataverse = this.commandEngine.submit(
        		new UpdateDataverseThemeCommand(dataverse, storage, request));
		this.userNotificationService.sendNotificationWithEmail((
				AuthenticatedUser) this.session.getUser(), 
				dataverse.getCreateDate(), CREATEDV, dataverse.getId(), 
				DATAVERSE);
		return dataverse;
	}

    public Either<DataverseError, Dataverse> saveEditedDataverse(Collection<DataverseFieldTypeInputLevel> dftilToBeSaved,
                                                                 Dataverse dataverse,
                                                                 DualListModel<DatasetFieldType> facets) {

        if (!dataverse.isFacetRoot()) {
            facets.getTarget().clear();
        }

        UpdateDataverseCommand cmd = new UpdateDataverseCommand(dataverse, facets.getTarget(), null,
                                                                requestService.getDataverseRequest(), Lists.newArrayList(dftilToBeSaved));

        try {
            dataverse = commandEngine.submit(cmd);
        } catch (CommandException ex) {
            return Either.left(new DataverseError(ex, BundleUtil.getStringFromBundle("dataverse.create.failure")));
        }

        return Either.right(dataverse);
    }
    
	public Dataverse saveEditedDataverse(Dataverse dataverse) {

		UpdateDataverseCommand cmd = new UpdateDataverseCommand(dataverse, null, null,
				requestService.getDataverseRequest(), null);
		return commandEngine.submit(cmd);
	}

    public Dataverse saveFeaturedDataverse(Dataverse dataverse, List<Dataverse> featuredDataverses) {
        return commandEngine.submit(new UpdateDataverseCommand(dataverse, null,
                                                               featuredDataverses,
                                                               requestService.getDataverseRequest(),
                                                               null));
    }

    /**
     * Operation to publish dataverse also known as release dataverse.
     */
    public Dataverse publishDataverse(Dataverse dataverseToBePublished) {

        return commandEngine.submit(new PublishDataverseCommand(requestService.getDataverseRequest(), dataverseToBePublished));
    }

    public void deleteDataverse(Dataverse dataverseToDelete){
        commandEngine.submit(new DeleteDataverseCommand(requestService.getDataverseRequest(), dataverseToDelete));
    }

}
