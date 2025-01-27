package edu.harvard.iq.dataverse.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;
import java.util.Optional;

/**
 * Base repository (data access) class for JPA entities.
 * @param <ID> type of entity identifier.
 * @param <T> type of entity.
 *
 * @author kaczynskid
 */
public abstract class JpaRepository<ID, T extends JpaEntity<ID>> implements JpaOperations<ID, T> {

    protected static final Logger log = LoggerFactory.getLogger(JpaRepository.class);

    private final Class<T> entityClass;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;

    // -------------------- CONSTRUCTORS --------------------

    public JpaRepository(final Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    // -------------------- LOGIC --------------------

    @Override
    public List<T> findAll() {
        final CriteriaBuilder builder = this.em.getCriteriaBuilder();
        final CriteriaQuery<T> query = builder.createQuery(this.entityClass);
        return this.em.createQuery(query).getResultList();
    }

    @Override
    public Long countAll() {
        final CriteriaBuilder builder = this.em.getCriteriaBuilder();
        final CriteriaQuery<Long> query = builder.createQuery(Long.class);
        query.select(builder.count(query.from(this.entityClass)));
        
        return this.em.createQuery(query).getSingleResult();
    }

    @Override
    public Optional<T> findById(final ID id) {
        return Optional.ofNullable(this.em.find(this.entityClass, id));
    }

    @Override
    public T getById(final ID id) {
        return findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        this.entityClass.getSimpleName() + " with ID " + id + " not found"));
    }
    
    @Override
    public T save(final T entity) {
        if (entity.isNew()) {
            this.em.persist(entity);
            this.em.flush();
            return entity;
        } else {
            return this.em.merge(entity);
        }
    }
    
    public T saveAndFlush(T entity) {     
        entity = save(entity);
        this.em.flush();
        return entity;
    }

    @Override
    public T saveFlushAndClear(T entity) {
        entity = saveAndFlush(entity);
        this.em.clear();
        return entity;
    }
    
    public void saveAll(final Iterable<T> entities) {
        entities.forEach(this::save);
    }

    @Override
    public void deleteById(final ID id) {
        delete(this.em.find(this.entityClass, id));
    }

    @Override
    public void delete(T entity) {
        this.em.remove(entity);
    }

    @Override
    public T refresh(T entity) {
        this.em.refresh(entity);
        return entity;
    }

    public void mergeAndDelete(T entity) {
        entity = this.em.merge(entity);
        delete(entity);
    }

    protected static <T> Optional<T> getSingleResult(final TypedQuery<T> query) {
        try {
            return Optional.of(query.getSingleResult());
        } catch (final NoResultException e) {
            return Optional.empty();
        }
    }
}
