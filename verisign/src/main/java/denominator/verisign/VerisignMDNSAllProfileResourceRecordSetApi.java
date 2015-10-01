package denominator.verisign;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.equal;
import static denominator.common.Util.nextOrNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import denominator.AllProfileResourceRecordSetApi;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.verisign.VerisignMDNSSaxEncoder.GetRRList;

final class VerisignMDNSAllProfileResourceRecordSetApi implements AllProfileResourceRecordSetApi {

  private final VerisignMDNS api;
  private final String zoneId;

  VerisignMDNSAllProfileResourceRecordSetApi(VerisignMDNS api, String zoneId) {
    this.api = api;
    this.zoneId = zoneId;
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {

    GetRRList getRRList = new GetRRList();
    getRRList.zoneName = zoneId;

    return new ResourceRecordByNameAndTypeIterator(api, getRRList);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {

    checkNotNull(name, "name");

    GetRRList getRRList = new GetRRList();
    getRRList.zoneName = zoneId;
    getRRList.ownerName = name;

    return new ResourceRecordByNameAndTypeIterator(api, getRRList);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {

    checkNotNull(name, "name");
    checkNotNull(type, "type");

    GetRRList getRRList = new GetRRList();
    getRRList.ownerName = name;
    getRRList.type = type;
    getRRList.zoneName = zoneId;

    return new ResourceRecordByNameAndTypeIterator(api, getRRList);
  }

  @Override
  public ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type, String qualifier) {

    checkNotNull(name, "name");
    checkNotNull(type, "type");
    checkNotNull(qualifier, "qualifier");

    GetRRList getRRList = new GetRRList();
    getRRList.ownerName = name;
    getRRList.type = type;
    getRRList.viewName = qualifier;
    getRRList.zoneName = zoneId;

    return nextOrNull(new ResourceRecordByNameAndTypeIterator(api, getRRList));
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
    List<Map<String, Object>> oldRData = null;

    Builder<Map<String, Object>> newRRSetBilder = ResourceRecordSet.builder();
    Builder<Map<String, Object>> deleteRRSetBilder = ResourceRecordSet.builder();


    if (oldRecordSet != null) {

      newRData = Lists.newArrayList(filter(rrset.records(), not(in(oldRecordSet.records()))));

      if (newRData.isEmpty() && !equal(oldRecordSet.ttl(), Integer.valueOf(ttlToApply))) {

        oldRData = new ArrayList<Map<String, Object>>();

        oldRData.addAll(oldRecordSet.records());


      } else if (newRData.isEmpty() && equal(oldRecordSet.ttl(), Integer.valueOf(ttlToApply))) {

        return;

      } else {

        List<Map<String, Object>> oldRDataList =
            ImmutableList.copyOf(filter(oldRecordSet.records(), in(rrset.records())));

        if (!oldRDataList.isEmpty()) {
          oldRData = new ArrayList<Map<String, Object>>();
          oldRData.addAll(oldRDataList);

          newRData.addAll(oldRDataList);

        }

      }

    } else {
      newRData = ImmutableList.copyOf(rrset.records());
    }

    if (newRData != null && !newRData.isEmpty()) {
      rrset =
          newRRSetBilder.name(rrset.name()).type(rrset.type()).ttl(ttlToApply).addAll(newRData)
              .build();
    }

    ResourceRecordSet<Map<String, Object>> deleteRRSet = null;

    if (oldRData != null) {
      deleteRRSetBilder.ttl(Integer.valueOf(ttlToApply));
      deleteRRSetBilder.name(oldRecordSet.name());
      deleteRRSetBilder.type(oldRecordSet.type());
      deleteRRSetBilder.addAll(oldRData);

      deleteRRSet = deleteRRSetBilder.build();
    }

    api.updateResourceRecords(zoneId, rrset, deleteRRSet);

  }

  @Override
  public void deleteByNameAndType(String name, String type) {

    checkNotNull(name, "name");
    checkNotNull(type, "type");

    try {
      ResourceRecordSet<?> oldRecordSet = nextOrNull(iterateByNameAndType(name, type));

      if (oldRecordSet != null) {
        api.deleteResourceRecords(zoneId, oldRecordSet);
      }
    } catch (VerisignMDNSException e) {
      if (!e.code().equalsIgnoreCase("ERROR_OPERATION_FAILURE")) {
        throw e;
      }
    }
  }

  @Override
  public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {

    checkNotNull(name, "name");
    checkNotNull(type, "type");
    checkNotNull(qualifier, "rdata for the record");

    ResourceRecordSet<Map<String, Object>> rrSet =
        ResourceRecordSet.builder().name(name).type(type).add(Util.toMap(type, qualifier)).build();
        
    try {
      api.deleteResourceRecords(zoneId, rrSet);
    } catch (VerisignMDNSException e) {
      if (!e.code().equalsIgnoreCase("ERROR_OPERATION_FAILURE")) {
        throw e;
      }
    }

  }

  static final class Factory implements denominator.AllProfileResourceRecordSetApi.Factory {

    private final VerisignMDNS api;

    @Inject
    Factory(VerisignMDNS api) {
      this.api = api;
    }

    @Override
    public VerisignMDNSAllProfileResourceRecordSetApi create(String id) {
      return new VerisignMDNSAllProfileResourceRecordSetApi(api, id);
    }
  }

}
