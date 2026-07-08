package edu.harvard.iq.dataverse.persistence.group;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.MocksFactory;

/**
 * @author michael
 */
public class IpGroupTest {


    /**
     * Test of isEditable method, of class IpGroup.
     */
    @Test
    public void testIsEditable() {
        IpGroup instance = new IpGroup();
        assertThat(instance.isEditable()).isTrue();
    }

    /**
     * Test of equals method, of class IpGroup.
     */
    @Test
    public void testEquals() {
        IpGroup group1 = new IpGroup();
        group1.setId(MocksFactory.nextId());
        group1.setDescription("A's description");
        group1.setDisplayName("A");
        group1.setPersistedGroupAlias("&ip/a");
        group1.add(IpAddressRange.make(IpAddress.valueOf("0.0.0.0"), IpAddress.valueOf("1.1.1.1")));

        assertThat(group1.equals("banana")).isFalse();
        assertThat(group1.equals(null)).isFalse();
        assertThat(group1.equals(group1)).isTrue();

        IpGroup group2 = new IpGroup();
        group2.setId(group1.getId());
        group2.setDescription("A's description");
        group2.setDisplayName("A");
        group2.setPersistedGroupAlias("&ip/a");
        group2.add(IpAddressRange.make(IpAddress.valueOf("0.0.0.0"), IpAddress.valueOf("1.1.1.1")));

        assertThat(group1.equals(group2)).isTrue();
        group2.add(IpAddressRange.make(IpAddress.valueOf("9.0.0.0"), IpAddress.valueOf("9.1.1.1")));
        assertThat(group1.equals(group2)).isFalse();

    }
    
    @Test
    public void containsAddress() {
    	
    	IpGroup group = new IpGroup();
    	IpAddressRange range4 = IpAddressRange.make(IpAddress.valueOf("0.0.0.0"), IpAddress.valueOf("1.1.1.1"));
    	IpAddressRange range6 = IpAddressRange.make(IpAddress.valueOf("0:0:0:0:0:0:0:0"), IpAddress.valueOf("0:0:0:0:0:0:0:1"));
    	
    	assertThat(group.containsAddress(range4.getTop())).isFalse(); 
    	assertThat(group.containsAddress(range6.getTop())).isFalse();
    	
    	
    	group.add(range4);
    	
    	assertThat(group.containsAddress(range4.getTop())).isTrue(); 
    	assertThat(group.containsAddress(range6.getTop())).isFalse();
    	
    	group.add(range6);
    	
    	assertThat(group.containsAddress(range4.getTop())).isTrue(); 
    	assertThat(group.containsAddress(range6.getTop())).isTrue();
    	
    	group.remove(range4);
    	
    	assertThat(group.containsAddress(range4.getTop())).isFalse(); 
    	assertThat(group.containsAddress(range6.getTop())).isTrue();
    	
    	
    	group.remove(range6);
    	
    	assertThat(group.containsAddress(range4.getTop())).isFalse(); 
    	assertThat(group.containsAddress(range6.getTop())).isFalse();
    }

}
