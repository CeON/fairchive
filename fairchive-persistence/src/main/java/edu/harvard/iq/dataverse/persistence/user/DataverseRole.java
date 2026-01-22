package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.common.BitSet;
import edu.harvard.iq.dataverse.common.RoleTranslationUtil;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static java.util.Arrays.stream;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

/**
 * A role is an annotated set of permissions. A role belongs
 * to a {@link Dataverse}. Users may assume roles from the current dataverse,
 * or from its parent dataverses, up to the first permission root dataverse.
 *
 * @author michael
 */
@SuppressWarnings("serial")
@Entity
@Table(indexes = {@Index(columnList = "owner_id")
        , @Index(columnList = "name")
        , @Index(columnList = "alias")})
public class DataverseRole implements Serializable, JpaEntity<Long> {

    //constants for the built in roles references in the code
    public enum BuiltInRole {
        ADMIN("admin"),
        FILE_DOWNLOADER("fileDownloader"),
        FULL_CONTRIBUTOR("fullContributor"),
        DV_CONTRIBUTOR("dvContributor"),
        DS_CONTRIBUTOR("dsContributor"),
        /**
         * Heads up that this says "editor" which comes from
         * scripts/api/data/role-editor.json but the name is "Contributor". The
         * *alias* is "editor". Don't be fooled!
         */
        EDITOR("editor"),
        CURATOR("curator"),
        MEMBER("member"),
        DEPOSITOR("depositor");
        
        private final String alias;
        
        BuiltInRole(final String alias) {
            this.alias = alias;
        }
        
        public String getAlias() {
            return this.alias;
        }
        
        public static BuiltInRole fromAlias(final String alias) {
            return stream(BuiltInRole.values())
                .filter(builtInRole -> builtInRole.getAlias().equals(alias))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
        }
    }

    public static final String NONE = "none";


    public static final Comparator<DataverseRole> CMP_BY_NAME = new Comparator<DataverseRole>() {

        @Override
        public int compare(DataverseRole o1, DataverseRole o2) {
            int cmp = o1.getName().compareTo(o2.getName());
            if (cmp != 0) {
                return cmp;
            }

            Long o1OwnerId = o1.getOwner() == null ? 0l : o1.getOwner().getId();
            Long o2OwnerId = o2.getOwner() == null ? 0l : o2.getOwner().getId();

            return o1OwnerId.compareTo(o2OwnerId);
        }
    };

    public static Set<Permission> permissionSet(final Iterable<DataverseRole> roles) {
        long miniset = 0l;
        for (final DataverseRole role : roles) {
            miniset |= role.permissionBits;
        }
        return new BitSet(miniset).asSetOf(Permission.class);
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Pattern(regexp = ".+", message = "{role.name}")
    @Column(nullable = false)
    private String name;

    @Size(max = 255, message = "{role.desc.maxLength}")
    private String description;

    @Size(max = 16, message = "{role.alias.maxLength}")
    @Pattern(regexp = "[a-zA-Z0-9\\_\\-]+", message = "{role.alias.illegalCharacters}")
    @Column(nullable = false, unique = true)
    private String alias;

    /**
     * Stores the permissions in a bit set.
     */
    private long permissionBits;

    @ManyToOne
    @JoinColumn(nullable = true)
    private DvObject owner;

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return RoleTranslationUtil.getLocaleNameFromAlias(this.alias, this.name);
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return RoleTranslationUtil.getLocaleDescriptionFromAlias(this.alias, this.description);
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(final String alias) {
        this.alias = alias;
    }

    public DvObject getOwner() {
        return this.owner;
    }

    public void setOwner(final DvObject owner) {
        this.owner = owner;
    }

    public void addPermissions(final Collection<Permission> ps) {
        for (final Permission p : ps) {
            addPermission(p);
        }
    }

    public void addPermission(final Permission p) {
        this.permissionBits = new BitSet(this.permissionBits).set(p.ordinal()).getBits();
    }

    public void clearPermissions() {
        this.permissionBits = 0l;
    }

    public Set<Permission> permissions() {
        return new BitSet(this.permissionBits).asSetOf(Permission.class);
    }

    public long getPermissionsBits() {
        return this.permissionBits;
    }

    @Override
    public String toString() {
        return "DataverseRole{" + "id=" + this.id + ", alias=" + this.alias + '}';
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DataverseRole other = (DataverseRole) obj;
        return Objects.equals(this.id, other.id);
    }

    /**
     * Given a DvObject object, see if this role contains a Permission
     * applicable to that object
     *
     * @param dvObject
     * @return
     */
    public boolean doesDvObjectHavePermissionForObject(final DvObject dvObject) {

        if (dvObject == null) {
            return false;
        }

        return this.doesDvObjectClassHavePermissionForObject(dvObject.getClass());

    } // doesDvObjectHavePermissionForObject


    /**
     * Given a DvObject object class, see if this role contains a Permission
     * applicable to that object
     * <p>
     * Initial user is for MyData page and displaying role tags
     *
     * @param dvObjectClass
     * @return
     */
    public boolean doesDvObjectClassHavePermissionForObject(final Class<? extends DvObject> dvObjectClass) {

        if (dvObjectClass == null) {
            return false;
        }

        // Iterate through permissions.  If one applies to this class, return true
        //
        for (final Permission perm : this.permissions()) {
            if (perm.appliesTo(dvObjectClass)) {
                return true;
            }
        }

        return false;

    } // doesDvObjectClassHavePermissionForObject
}
