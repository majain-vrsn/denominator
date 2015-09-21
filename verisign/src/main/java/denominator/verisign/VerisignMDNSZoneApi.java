package denominator.verisign;

import static denominator.common.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import denominator.model.Zone;
import denominator.verisign.VerisignMDNSContentHandlers.Page;
import denominator.verisign.VerisignMDNSSaxEncoder.Paging;

class VerisignMDNSZoneApi implements denominator.ZoneApi {

  private static final int PAGE_SIZE = 100;

  private final VerisignMDNS api;

  @Inject
  VerisignMDNSZoneApi(VerisignMDNS api) {
    this.api = api;
  }

  @Override
  public Iterator<Zone> iterator() {

    final Page<Zone> page =
        api.getZones(paging(1));

    final int pages = (page.count / PAGE_SIZE) + 1;

    final Iterator<Zone> currentIterator = page.list.iterator();

    if (pages == 1) {
      return currentIterator;
    }
    return new Iterator<Zone>() {
      Iterator<Zone> current = currentIterator;
      int i = 1;

      @Override
      public boolean hasNext() {
        while (!current.hasNext() && i <= pages) {

          Page<Zone> page = api.getZones(paging(++i));
          current = page.list.iterator();
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

    /*
    while (zoneIterator != null && zoneIterator.hasNext()) {

      Zone oldZone = zoneIterator.next();

      if (!oldZone.name().equals(zone.name())) {

        isUpdate = true;

        CloneZoneType cloneZone = new CloneZoneType();
        cloneZone.setDomainNameToClone(oldZone.name());
        cloneZone.setDomainName(zone.name());

        api.cloneZone(new ObjectFactory().createCloneZone(cloneZone));

        delete(oldZone.name());
      }

    }
    */

    if (!isUpdate) {
      api.createZone(zone);
    }

    return zone.name();

  }


  @Override
  public void delete(String zone) {
    checkNotNull(zone, "zone");
    api.deleteZone(zone);

  }

  @Override
  public Iterator<Zone> iterateByName(String name) {

    checkNotNull(name, "zoneName");
    
    Iterator<Zone> iterator = iterator();
    
    List<Zone> zones = new ArrayList<Zone>();
    
    while(iterator.hasNext()) {
      
      Zone next = iterator.next();
      
      if(name.equalsIgnoreCase(next.name())) {
        zones.add(next);
      }
      
    }

    return zones.iterator();
  }


  private Paging paging(int pageNumber) {

    Paging paging = new Paging();
    paging.pageSize = PAGE_SIZE;
    paging.pageNumber = pageNumber;

    return paging;

  }

}
