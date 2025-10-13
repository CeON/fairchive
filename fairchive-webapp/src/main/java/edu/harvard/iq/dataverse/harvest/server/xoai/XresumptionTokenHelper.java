package edu.harvard.iq.dataverse.harvest.server.xoai;


import org.dspace.xoai.model.oaipmh.ResumptionToken;

import static com.google.common.base.Predicates.isNull;

/**
 * @author Leonid Andreev
 * Dataverse's own version of the XOAI ResumptionTokenHelper
 * Fixes the issue with the offset cursor: the OAI validation spec
 * insists that it starts with 0, while the XOAI implementation uses 1
 * as the initial offset.
 */
final class XresumptionTokenHelper {

    private ResumptionToken.Value current;
    private long maxPerPage;
    private Long totalResults;

    XresumptionTokenHelper(ResumptionToken.Value current, long maxPerPage) {
        this.current = current;
        this.maxPerPage = maxPerPage;
    }

    ResumptionToken resolve(boolean hasMoreResults) {
        if (isInitialOffset() && !hasMoreResults) {
            return null;
        } else {
            if (hasMoreResults) {
                ResumptionToken.Value next = current.next(maxPerPage);
                return populate(new ResumptionToken(next));
            } else {
                ResumptionToken resumptionToken = new ResumptionToken();
                resumptionToken.withCursor(current.getOffset());
                if (totalResults != null) {
                    resumptionToken.withCompleteListSize(totalResults);
                }
                return resumptionToken;
            }
        }
    }

    private boolean isInitialOffset() {
        return isNull().apply(current.getOffset()) || current.getOffset() == 0;
    }

    private ResumptionToken populate(ResumptionToken resumptionToken) {
        if (totalResults != null) {
            resumptionToken.withCompleteListSize(totalResults);
        }
        resumptionToken.withCursor(resumptionToken.getValue().getOffset() - maxPerPage);
        return resumptionToken;
    }


}
