package denominator.verisign;

import static denominator.common.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Iterator;

import javax.inject.Inject;

import mdns.wsdl.CloneZoneType;
import mdns.wsdl.CreateZoneType;
import mdns.wsdl.DeleteZoneType;
import mdns.wsdl.GetZoneInfoResTypeV2;
import mdns.wsdl.GetZoneInfoTypeV2;
import mdns.wsdl.GetZoneListResType;
import mdns.wsdl.GetZoneListType;
import mdns.wsdl.ListPagingInfo;
import mdns.wsdl.ObjectFactory;
import mdns.wsdl.ZoneInfo;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import denominator.model.Zone;

class VerisignMDNSZoneApi implements denominator.ZoneApi {
	
	private static final int PAGE_SIZE = 100;
	
    private final VerisignMDNS api;

    @Inject
    VerisignMDNSZoneApi(VerisignMDNS api) {
        this.api = api;
    }
    
    @Override
    public Iterator<Zone> iterator() {
    	
    	final ObjectFactory objectFactory = new ObjectFactory();
    	
    	final GetZoneListResType response = api.getZones(objectFactory.createGetZoneList(getZoneRequestObj(1)));
    	
    	final int pages = (response.getTotalCount() / PAGE_SIZE) + 1;
    	
    	final ZoneInfoTransformer transformer = new ZoneInfoTransformer();
    	
    	final Iterator<Zone> currentIterator = Lists.transform(response.getZoneInfo(), transformer).iterator();
    	
        if (pages == 1) {
            return currentIterator;
        }
        return new Iterator<Zone>() {
            Iterator<Zone> current = currentIterator;
            int i = 1;

            @Override
            public boolean hasNext() {
                while (!current.hasNext() && i <= pages) {
                	
                	GetZoneListResType response = api.getZones(objectFactory.createGetZoneList(getZoneRequestObj(++i)));
                	
                    current = Lists.transform(response.getZoneInfo(), transformer).iterator();
                }
                
                return current.hasNext();
            }

            @Override
            public Zone next() {
                return current.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    @Override
	public String put(Zone zone) {
    	
    	checkNotNull(zone, "zone");
    	checkNotNull(zone.name(), "zoneName");
    	
    	Iterator<Zone> zoneIterator = iterateByName(zone.name());
    	
    	boolean isUpdate = false;
    	    	
    	while(zoneIterator != null && zoneIterator.hasNext()) {
    		
    		Zone oldZone = zoneIterator.next();
    		
    		if(!oldZone.name().equals(zone.name())) {
    			
    			isUpdate = true;
    			
    			CloneZoneType cloneZone = new CloneZoneType();
    	    	cloneZone.setDomainNameToClone(oldZone.name());
    	    	cloneZone.setDomainName(zone.name());
    	    	
    	    	api.cloneZone(new ObjectFactory().createCloneZone(cloneZone));
    	    	
    	    	delete(oldZone.name());
    		}
    		
    	}
    	
    	if(!isUpdate) {
			CreateZoneType createZoneType = new CreateZoneType();
			createZoneType.setDomainName(zone.name());
			createZoneType.setType(mdns.wsdl.ZoneType.DNS_HOSTING);
			
			api.createZone(new ObjectFactory().createCreateZone(createZoneType));
    	}
		
		return zone.name();
		
	}


	@Override
	public void delete(String zone) {
		
		checkNotNull(zone, "zone");
		
		DeleteZoneType deleteZoneType = new DeleteZoneType();
		deleteZoneType.setDomainName(zone);
		
		api.deleteZone(new ObjectFactory().createDeleteZone(deleteZoneType));
		
	}
	
	@Override
	public Iterator<Zone> iterateByName(String name) {
		
		checkNotNull(name, "zoneName");
		
		GetZoneInfoTypeV2 request = new GetZoneInfoTypeV2();
		request.setDomainName(name);
		request.setExcludeResourceRecords(true);
		
		GetZoneInfoResTypeV2 zoneInfoRes = api.getZone(new ObjectFactory().createGetZoneInfoV2(request));
		
		ZoneInfoTransformer transformer = new ZoneInfoTransformer();
		
		ZoneInfo zoneInfo = zoneInfoRes.getPrimaryZoneInfo() != null ? zoneInfoRes.getPrimaryZoneInfo() : zoneInfoRes.getSecondaryZoneInfo();
		
		return zoneInfo != null ? Collections.singletonList(transformer.apply(zoneInfo)).iterator()  : null;
	}

    
    private GetZoneListType getZoneRequestObj(int pageNumber) {
    	
    	GetZoneListType request = new GetZoneListType();
    	
    	ListPagingInfo paging = new ListPagingInfo();
    	paging.setPageSize(PAGE_SIZE);
    	paging.setPageNumber(pageNumber);
    	
    	request.setListPagingInfo(paging);
    	
    	return request;
    	
    }
    
    public static class ZoneInfoTransformer implements Function<ZoneInfo, Zone> {
		
		@Inject
		ZoneInfoTransformer() { }
		
		@Override
		public Zone apply(ZoneInfo input) {
			
			Zone zone = Zone.create(input.getDomainName(), input.getDomainName(), 86400, "nil@" + input.getDomainName());
			
			return zone;
		}
		
	}


}
