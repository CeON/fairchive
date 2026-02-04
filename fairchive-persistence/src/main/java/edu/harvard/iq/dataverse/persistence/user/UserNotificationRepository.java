package edu.harvard.iq.dataverse.persistence.user;


import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.google.common.collect.Lists;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

/**
 * @author xyang
 */
@Stateless
public class UserNotificationRepository extends JpaRepository<Long, UserNotification> {
    private final static int DELETE_BATCH_SIZE = 100;

    // -------------------- CONSTRUCTORS --------------------

    public UserNotificationRepository() {
        super(UserNotification.class);
    }

    // -------------------- LOGIC --------------------

    public UserNotificationQueryResult query(final UserNotificationQuery queryParam) {

        final CriteriaBuilder builder = em.getCriteriaBuilder();
        final CriteriaQuery<UserNotification> query = builder.createQuery(UserNotification.class);
        final Root<UserNotification> root = query.from(UserNotification.class);

        final List<Predicate> predicates = new ArrayList<>();
        if (isNotBlank(queryParam.getSearchLabel())) {
            predicates.add(builder.like(root.get("searchLabel"),
            		"%" + queryParam.getSearchLabel().toLowerCase() + "%"));
        }

        if (queryParam.getUserId() != null) {
            predicates.add(builder.equal(root.get("user").get("id"), 
            		queryParam.getUserId()));
        }

        query.select(root)
                .where(predicates.toArray(new Predicate[]{}))
                .orderBy(queryParam.isAscending() 
                		? builder.asc(root.get("sendDate")) 
                		: builder.desc(root.get("sendDate")));

        final List<UserNotification> resultList = this.em.createQuery(query)
                .setFirstResult(queryParam.getOffset())
                .setMaxResults(queryParam.getResultLimit())
                .getResultList();

        final CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
        countQuery.select(builder.count(root))
                .where(predicates.toArray(new Predicate[]{}));

        final Long totalCount = this.em.createQuery(countQuery).getSingleResult();

        return new UserNotificationQueryResult(resultList, totalCount);
    }

    public List<UserNotification> findByUser(final Long userId) {
        return this.em.createQuery(
        		"select un from UserNotification un " +
                 "where un.user.id =:id order by un.sendDate desc", 
                 UserNotification.class)
                .setParameter("id", userId)
                .getResultList();
    }

    public int updateRequestor(final Long oldId, final Long newId) {
        return this.em.createNativeQuery(format("update usernotification " +
                "set parameters = jsonb_set(parameters::jsonb, '{requestorId}', '\"%s\"')::json " +
                "where parameters ->> 'requestorId' = '%s'", newId.toString(), oldId.toString()))
                .executeUpdate();
    }

    public int deleteByIds(final Set<Long> ids) {
        return Lists.partition(Lists.newArrayList(ids), DELETE_BATCH_SIZE).stream()
                .mapToInt(idBatch ->
                        this.em.createQuery(
                        		"delete from UserNotification where id in :ids",
                        		UserNotification.class)
                                .setParameter("ids", idBatch)
                                .executeUpdate())
                .sum();
    }

    public int deleteByUser(Long userId) {
        return this.em.createQuery("delete from UserNotification where user.id = :id", 
        		UserNotification.class)
                .setParameter("id", userId)
                .executeUpdate();
    }

    public Long getUnreadNotificationCountByUser(final Long userId) {
        return this.em.createQuery(
        		"select count(un) from UserNotification as un " +
                "where un.user.id = :id and un.readNotification = false",
                Long.class)
                .setParameter("id", userId)
                .getSingleResult();
    }

    public int updateEmailSent(long userNotificationId) {
        return this.em.createQuery(
        		"UPDATE UserNotification notification " + 
                "SET notification.emailed = :emailSent " +
                "WHERE notification.id = :id")
                .setParameter("emailSent", true)
                .setParameter("id", userNotificationId)
                .executeUpdate();
    }

    public UserNotification findLastSubmitNotificationByObjectId(final long id) {
        final List<UserNotification> notifications = this.em.createQuery(
        		"SELECT un FROM UserNotification un " +
                "WHERE un.objectId = :id AND un.type = :type " +
                "ORDER BY un.sendDate DESC", UserNotification.class)
                .setParameter("id", id)
                .setParameter("type", NotificationType.SUBMITTEDDS)
                .getResultList();
        return notifications.isEmpty() ? null : notifications.get(0);
    }

}
