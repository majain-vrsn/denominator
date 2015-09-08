package denominator.verisign;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.equal;
import static denominator.common.Util.nextOrNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import mdns.wsdl.BulkUpdateSingleZone;
import mdns.wsdl.GetResourceRecordListType;
import mdns.wsdl.ObjectFactory;
import mdns.wsdl.ResourceRecordDataType;
import mdns.wsdl.ResourceRecordType;
import mdns.wsdl.ResourceRecordsType;
import mdns.wsdl.UniqueResourceRecordDataType;
import mdns.wsdl.UniqueResourceRecordsType;
import mdns.wsdl.UpdateResourceRecordType;
import mdns.wsdl.UpdateResourceRecordsType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import denominator.AllProfileResourceRecordSetApi;
import denominator.Provider;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;

final class VerisignMDNSAllProfileResourceRecordSetApi implements AllProfileResourceRecordSetApi {

  public static final String MDNS_RESOURCE_RECORD_LIMIT_ERROR_MSG =
      "Resource record limit exceeded";

  private final VerisignMDNS api;
  private final String zoneId;
  private final Integer resourceRecordLimit;

  VerisignMDNSAllProfileResourceRecordSetApi(VerisignMDNS api, String zoneId,
      Integer resourceRecordLimit) {
    this.api = api;
    this.zoneId = zoneId;
    this.resourceRecordLimit = resourceRecordLimit;
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    ResourceRecordSet<?> oldRecordSet = null;

    int ttlToApply = rrset.ttl() != null ? rrset.ttl() : 86400;

    if (rrset.qualifier() != null) {
      oldRecordSet = getByNameTypeAndQualifier(rrset.name(), rrset.type(), rrset.qualifier());
    } else {
      oldRecordSet = nextOrNull(iterateByNameAndType(rrset.name(), rrset.type()));
    }

    List<Map<String, Object>> newRData = null;

    ResourceRecordsType createResourceRecords = null;

    UpdateResourceRecordsType updateResourceRecords = null;

    UniqueResourceRecordsType deleteResourceRecords = null;

    if (oldRecordSet != null) {

      newRData = Lists.newArrayList(filter(rrset.records(), not(in(oldRecordSet.records()))));

      if (newRData.isEmpty() && !equal(oldRecordSet.ttl(), Integer.valueOf(ttlToApply))) {

        updateResourceRecords = new UpdateResourceRecordsType();

        for (Map<String, Object> data : oldRecordSet.records()) {

          UpdateResourceRecordType updateResourceRecord = new UpdateResourceRecordType();

          UniqueResourceRecordDataType oldResourceRecord = new UniqueResourceRecordDataType();
          oldResourceRecord.setOwner(oldRecordSet.name());
          oldResourceRecord.setType(ResourceRecordType.valueOf(oldRecordSet.type()));
          oldResourceRecord.setRData(Util.flatten(data));

          updateResourceRecord.setOldResourceRecord(oldResourceRecord);

          ResourceRecordDataType newResourceRecord = new ResourceRecordDataType();
          newResourceRecord.setOwner(oldRecordSet.name());
          newResourceRecord.setType(ResourceRecordType.valueOf(oldRecordSet.type()));
          newResourceRecord.setTtl(Long.valueOf(ttlToApply));
          newResourceRecord.setRData(Util.flatten(data));

          updateResourceRecord.setNewResourceRecord(newResourceRecord);

          updateResourceRecords.getUpdateResourceRecord().add(updateResourceRecord);

        }

      } else if (newRData.isEmpty() && equal(oldRecordSet.ttl(), Integer.valueOf(ttlToApply))) {

        return;

      } else {

        List<Map<String, Object>> oldRData =
            ImmutableList.copyOf(filter(oldRecordSet.records(), in(rrset.records())));

        if (!oldRData.isEmpty()) {

          deleteResourceRecords = new UniqueResourceRecordsType();

          for (Map<String, Object> data : oldRData) {

            UniqueResourceRecordDataType oldResourceRecord = new UniqueResourceRecordDataType();
            oldResourceRecord.setOwner(oldRecordSet.name());
            oldResourceRecord.setType(ResourceRecordType.valueOf(oldRecordSet.type()));
            oldResourceRecord.setRData(Util.flatten(data));

            deleteResourceRecords.getResourceRecord().add(oldResourceRecord);

          }

          newRData.addAll(oldRData);

        }

      }

    } else {
      newRData = ImmutableList.copyOf(rrset.records());
    }

    if (newRData != null && !newRData.isEmpty()) {

      createResourceRecords = new ResourceRecordsType();

      for (Map<String, Object> data : newRData) {

        ResourceRecordDataType rr = new ResourceRecordDataType();
        rr.setOwner(rrset.name());
        rr.setType(ResourceRecordType.valueOf(rrset.type()));
        rr.setTtl(Long.valueOf(ttlToApply));
        rr.setRData(Util.flatten(data));

        createResourceRecords.getResourceRecord().add(rr);

      }
    }

    BulkUpdateSingleZone updateSingleZone = new BulkUpdateSingleZone();
    updateSingleZone.setDomainName(zoneId);

    if (deleteResourceRecords != null) {
      updateSingleZone.setDeleteResourceRecords(deleteResourceRecords);
    }

    if (updateResourceRecords != null) {
      updateSingleZone.setUpdateResourceRecords(updateResourceRecords);
    }

    if (createResourceRecords != null) {
      updateSingleZone.setCreateResourceRecords(createResourceRecords);
    }

    api.updateResourceRecords(new ObjectFactory().createBulkUpdateSingleZone(updateSingleZone));
  }

  private UniqueResourceRecordsType getUniqueResourceRecordsType(ResourceRecordSet<?> rrSet) {

    UniqueResourceRecordsType uniqueResourceRecordsType = new UniqueResourceRecordsType();

    for (Map<String, Object> data : rrSet.records()) {

      UniqueResourceRecordDataType oldResourceRecord = new UniqueResourceRecordDataType();
      oldResourceRecord.setOwner(rrSet.name());
      oldResourceRecord.setType(ResourceRecordType.valueOf(rrSet.type()));
      oldResourceRecord.setRData(Util.flatten(data));

      uniqueResourceRecordsType.getResourceRecord().add(oldResourceRecord);

    }

    return uniqueResourceRecordsType;

  }


  @Override
  public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {

    checkNotNull(name, "name");
    checkNotNull(type, "type");
    checkNotNull(qualifier, "rdata for the record");

    UniqueResourceRecordsType deleteResourceRecord = new UniqueResourceRecordsType();

    UniqueResourceRecordDataType oldResourceRecord = new UniqueResourceRecordDataType();
    oldResourceRecord.setOwner(name);
    oldResourceRecord.setType(ResourceRecordType.valueOf(type));
    oldResourceRecord.setRData(qualifier);

    deleteResourceRecord.getResourceRecord().add(oldResourceRecord);

    BulkUpdateSingleZone updateSingleZone = new BulkUpdateSingleZone();
    updateSingleZone.setDomainName(zoneId);
    updateSingleZone.setDeleteResourceRecords(deleteResourceRecord);

    api.updateResourceRecords(new ObjectFactory().createBulkUpdateSingleZone(updateSingleZone));

  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    GetResourceRecordListType rrListType = new GetResourceRecordListType();
    rrListType.setDomainName(zoneId);

    return new ResourceRecordByNameAndTypeIterator(api, rrListType);

  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {

    checkNotNull(name, "name");

    GetResourceRecordListType rrListType = new GetResourceRecordListType();
    rrListType.setDomainName(zoneId);
    rrListType.setOwner(name);

    return new ResourceRecordByNameAndTypeIterator(api, rrListType);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {

    checkNotNull(name, "name");
    checkNotNull(type, "type");

    GetResourceRecordListType rrListType = new GetResourceRecordListType();
    rrListType.setDomainName(zoneId);
    rrListType.setOwner(name);
    rrListType.setResourceRecordType(ResourceRecordType.fromValue(type));

    return new ResourceRecordByNameAndTypeIterator(api, rrListType);
  }

  @Override
  public ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type, String qualifier) {

    checkNotNull(name, "name");
    checkNotNull(type, "type");
    checkNotNull(qualifier, "qualifier");

    GetResourceRecordListType rrListType = new GetResourceRecordListType();
    rrListType.setDomainName(zoneId);
    rrListType.setOwner(name);
    rrListType.setResourceRecordType(ResourceRecordType.fromValue(type));
    rrListType.setViewName(qualifier);

    return nextOrNull(new ResourceRecordByNameAndTypeIterator(api, rrListType));
  }

  @Override
  public void deleteByNameAndType(String name, String type) {

    checkNotNull(name, "name");
    checkNotNull(type, "type");

    ResourceRecordSet<?> oldRecordSet = nextOrNull(iterateByNameAndType(name, type));

    if (oldRecordSet != null) {

      UniqueResourceRecordsType deleteResourceRecord = getUniqueResourceRecordsType(oldRecordSet);

      BulkUpdateSingleZone updateSingleZone = new BulkUpdateSingleZone();
      updateSingleZone.setDomainName(zoneId);
      updateSingleZone.setDeleteResourceRecords(deleteResourceRecord);

      api.updateResourceRecords(new ObjectFactory().createBulkUpdateSingleZone(updateSingleZone));

    }

  }

  private int getTotalRRCount(GetResourceRecordListType rrListType) {
    ResourceRecordByNameAndTypeIterator rrIterator =
        new ResourceRecordByNameAndTypeIterator(0, 2, api, rrListType);
    return rrIterator.hasNext() ? rrIterator.getTotalCount() : 0;
  }

  static final class Factory implements denominator.AllProfileResourceRecordSetApi.Factory {

    private final VerisignMDNS api;

    private final VerisignMDNSProvider provider;

    @Inject
    Factory(VerisignMDNS api, Provider provider) {
      this.api = api;
      this.provider = (VerisignMDNSProvider) provider;
    }

    @Override
    public VerisignMDNSAllProfileResourceRecordSetApi create(String id) {
      return new VerisignMDNSAllProfileResourceRecordSetApi(api, id,
          provider.getResourceRecordLimit());
    }
  }

}
