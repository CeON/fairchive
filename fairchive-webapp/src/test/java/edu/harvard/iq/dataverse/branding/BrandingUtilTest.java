package edu.harvard.iq.dataverse.branding;

import static edu.harvard.iq.dataverse.common.BrandingUtil.getContactHeader;
import static edu.harvard.iq.dataverse.common.BrandingUtil.getInstallationBrandName;
import static edu.harvard.iq.dataverse.common.BrandingUtil.getSupportTeamEmailAddress;
import static edu.harvard.iq.dataverse.common.BrandingUtil.getSupportTeamName;
import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.UnsupportedEncodingException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.junit.jupiter.api.Test;

public class BrandingUtilTest {

    @Test
    public void testGetInstallationBrandName() {
    	
        assertEquals("LibraScholar", getInstallationBrandName("LibraScholar"));
        assertEquals(null, getInstallationBrandName(null));// misconfiguration to set to null
        assertEquals("", getInstallationBrandName(""));// misconfiguration to set to empty string
    }

    @Test
    public void testGetSupportTeamName() 
    		throws AddressException, UnsupportedEncodingException {
    	
        assertEquals("Support", getSupportTeamName(null, null));
        assertEquals("Support", getSupportTeamName(null, ""));
        assertEquals("LibraScholar Support", getSupportTeamName(null, "LibraScholar"));
        assertEquals("LibraScholar Support", getSupportTeamName(new InternetAddress("support@librascholar.edu"), "LibraScholar"));
        assertEquals("LibraScholar Support Team", getSupportTeamName(new InternetAddress("support@librascholar.edu", "LibraScholar Support Team"), "LibraScholar"));
        assertEquals("", getSupportTeamName(new InternetAddress("support@librascholar.edu", ""), "LibraScholar")); // misconfiguration to set to empty string
    }

    @Test
    public void testGetSupportEmailAddress() 
    		throws AddressException, UnsupportedEncodingException {
    	
        assertEquals(null, getSupportTeamEmailAddress(null));
        assertEquals("support@librascholar.edu", getSupportTeamEmailAddress(new InternetAddress("support@librascholar.edu")));
        assertEquals("support@librascholar.edu", getSupportTeamEmailAddress(new InternetAddress("support@librascholar.edu", "LibraScholar Support Team")));
        assertEquals("support@librascholar.edu", getSupportTeamEmailAddress(new InternetAddress("support@librascholar.edu", ""))); // misconfiguration to set to empty string but doesn't matter
        assertEquals(null, getSupportTeamEmailAddress(new InternetAddress(null, "LibraScholar Support Team"))); // misconfiguration to set to null
        assertEquals("", getSupportTeamEmailAddress(new InternetAddress("", "LibraScholar Support Team"))); // misconfiguration to set to empty string
    }

    @Test
    public void testWelcomeInAppNotification() {
    	
        String message = getStringFromBundle("notification.welcome",
                                                                "LibraScholar",
                                                                "<a href=\"http://guides.dataverse.org/en/4.3/user/index.html\">User Guide</a>",
                                                                "<a href=\"https://demo.dataverse.org\">Demo Site</a>"
                                                        );
        assertEquals("Welcome to LibraScholar! Get started by adding or finding data. " +
                        "Have questions? Check out the <a href=\"http://guides.dataverse.org/en/4.3/user/index.html\">User Guide</a>. " +
                        "Also, check for your welcome email to verify your address.",
                     message);
    }

    @Test
    public void testWelcomeEmail() {
    	
        String message = getStringFromBundle("notification.email.welcome",
                                                                "LibraScholar",
                                                                "http://guides.librascholar.edu/en",
                                                                "4.3",
                                                                "LibraScholar Support",
                                                                "support@librascholar.edu"
                                                        );
        assertEquals("Welcome to LibraScholar! "
                             + "Have questions? Check out the User Guide at http://guides.librascholar.edu/en/4.3/user/index.html"
                             + " or ask for assistance by writing at support@librascholar.edu .",
                     message);
    }

    @Test
    public void testEmailClosing() {
    	
        String message = getStringFromBundle("notification.email.closing",
                                                                "support@librascholar.edu",
                                                                "LibraScholar Support Team"
                                                        );
        assertEquals("\n\nYou may contact us for support at support@librascholar.edu.\n\nThank you,\nLibraScholar Support Team",
                     message);
    }

    @Test
    public void testEmailSubject() {
    	
        String message = getStringFromBundle("notification.email.create.account.subject",
                                                                "LibraScholar"
                                                        );
        assertEquals("LibraScholar: Your account has been created",
                     message);
    }

    @Test
    public void testGetContactHeader() {
    	
        assertEquals("Contact the Repository's Support", getContactHeader());
    }

}
