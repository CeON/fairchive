/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.persistence.guestbook;

import java.io.Serializable;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

/**
 * @author skraffmiller
 */
@SuppressWarnings("serial")
@Stateless
public class GuestbookRepository extends JpaRepository<Long, Guestbook> implements Serializable  {
    
    public GuestbookRepository() {
        super(Guestbook.class);
    }

    public Long findCountUsages(final Long guestbookId, final Long dataverseId) {
        if (guestbookId != null && dataverseId != null) {
            return (Long) this.em.createNativeQuery(
                    "select count(o.id) from Dataset  o, DvObject obj  " +
                    "where o.id = obj.id and  o.guestbook_id  = " + guestbookId +
                    " and obj.owner_id = " + dataverseId)
                    .getSingleResult();
        } else if (guestbookId != null && dataverseId == null) {
            return (Long) this.em.createNativeQuery(
                    "select count(o.id) from Dataset  o  " + 
                    "where o.guestbook_id  = " + guestbookId)
                    .getSingleResult();
        } else {
            return 0L;
        }
    }

    public Guestbook find(final Object pk) {
        return this.em.find(Guestbook.class, pk);
    }

}
