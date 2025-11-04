package edu.harvard.iq.dataverse.authorization.groups.impl.mail;

import edu.harvard.iq.dataverse.persistence.group.MailDomainGroup;
import edu.harvard.iq.dataverse.persistence.group.MailDomainItem;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Singleton
public class MailDomainCachedMatcherBean {

    private Map<String, Set<MailDomainGroup>> matchIndex = new HashMap<>();

    // -------------------- CONSTRUCTORS --------------------

    public MailDomainCachedMatcherBean() { }

    // -------------------- LOGIC --------------------

    @Lock
    public Set<MailDomainGroup> matchGroupsForDomain(final String domain) {
        final Set<String> allDomainSubstrings = createAllDomainSubstrings(domain);

        // First look for allowed groups
        final Set<MailDomainGroup> allowedGroups = allDomainSubstrings.stream()
                .map(s -> this.matchIndex.getOrDefault(s, emptySet()))
                .flatMap(Collection::stream)
                .collect(toSet());

        // Then check if we should exclude user from one of found groups
        final Set<MailDomainGroup> toDisallow = allowedGroups.stream()
                .flatMap(MailDomainGroup::getExclusionsStream)
                .filter(i -> allDomainSubstrings.contains(i.getDomain()))
                .map(MailDomainItem::getOwner)
                .collect(toSet());

        allowedGroups.removeAll(toDisallow);
        return allowedGroups;
    }

    @Lock(LockType.WRITE)
    public void rebuildIndex(final Collection<MailDomainGroup> groups) {
        this.matchIndex = groups.stream()
                .flatMap(MailDomainGroup::getInclusionsStream)
                .collect(groupingBy(
                        MailDomainItem::getDomain,
                        mapping(MailDomainItem::getOwner, toSet())));
    }

    // -------------------- PRIVATE --------------------

    /**
     * Creates a set consisting of the given domain and subdomain strings
     * contained in it.
     * <br>
     * E.g. for icm.uw.edu.pl produces [icm.uw.edu.pl, .uw.edu.pl, .edu.pl, .pl]
     */
    private Set<String> createAllDomainSubstrings(final String domain) {
        final Set<String> domainSubstrings = new HashSet<>();
        domainSubstrings.add(domain);
        for (int i = domain.indexOf('.');
             i >= 0 && i < domain.length();
             i = domain.indexOf('.', i + 1)) {
            domainSubstrings.add(domain.substring(i));
        }
        return domainSubstrings;
    }
}
