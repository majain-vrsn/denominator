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
import denominator.verisign.VerisignMDNSEncoder.GetRRList;

public final class VerisignMDNSAllProfileResourceRecordSetApi implements
    AllProfileResourceRecordSetApi {

  private final VerisignMDNS api;
  private final String zoneName;

  VerisignMDNSAllProfileResourceRecordSetApi(VerisignMDNS api, String zoneName) {
    this.api = api;
    this.zoneName = zoneName;
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    GetRRList getRRList = new GetRRList();
    getRRList.zoneName = zoneName;
    return new ResourceRecordByNameAndTypeIterator(api, getRRList);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    checkNotNull(name, "name");
    GetRRList getRRList = new GetRRList();
    getRRList.zoneName = zoneName;
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
    getRRList.zoneName = zoneName;
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
    getRRList.zoneName = zoneName;

    return nextOrNull(new ResourceRecordByNameAndTypeIterator(api, getRRList));
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    checkNotNull(rrset, "rrset was null");
    int ttlToApply = rrset.ttl() != null ? rrset.ttl() : 86400;

    ResourceRecordSet<?> oldRRSet = null;
    if (rrset.qualifier() != null) {
      oldRRSet = getByNameTypeAndQualifier(rrset.name(), rrset.type(), rrset.qualifier());
    } else {
      oldRRSet = nextOrNull(iterateByNameAndType(rrset.name(), rrset.type()));
    }

    List<Map<String, Object>> newRRData = null;
    List<Map<String, Object>> oldRRData = null;
    Builder<Map<String, Object>> newRRSetBuilder = ResourceRecordSet.builder();
    Builder<Map<String, Object>> deleteRRSetBuilder = ResourceRecordSet.builder();

    if (oldRRSet != null) {
      newRRData = Lists.newArrayList(filter(rrset.records(), not(in(oldRRSet.records()))));
      if (newRRData.isEmpty() && !equal(oldRRSet.ttl(), Integer.valueOf(ttlToApply))) {
        oldRRData = new ArrayList<Map<String, Object>>();
        oldRRData.addAll(oldRRSet.records());
      } else if (newRRData.isEmpty() && equal(oldRRSet.ttl(), Integer.valueOf(ttlToApply))) {
        return;
      } else {
        List<Map<String, Object>> oldRRDataList =
            ImmutableList.copyOf(filter(oldRRSet.records(), in(rrset.records())));

        if (!oldRRDataList.isEmpty()) {
          oldRRData = new ArrayList<Map<String, Object>>();
          oldRRData.addAll(oldRRDataList);
          newRRData.addAll(oldRRDataList);
        }
      }
    } else {
      newRRData = ImmutableList.copyOf(rrset.records());
    }

    if (newRRData != null && !newRRData.isEmpty()) {
      rrset =
          newRRSetBuilder.name(rrset.name()).type(rrset.type()).ttl(ttlToApply).addAll(newRRData)
              .build();
    }

    ResourceRecordSet<Map<String, Object>> deleteRRSet = null;
    if (oldRRData != null) {
      deleteRRSetBuilder.ttl(Integer.valueOf(ttlToApply));
      deleteRRSetBuilder.name(oldRRSet.name());
      deleteRRSetBuilder.type(oldRRSet.type());
      deleteRRSetBuilder.addAll(oldRRData);
      deleteRRSet = deleteRRSetBuilder.build();
    }

    api.updateResourceRecords(zoneName, rrset, deleteRRSet);
  }

  @Override
  public void deleteByNameAndType(String name, String type) {
    checkNotNull(name, "name");
    checkNotNull(type, "type");
    try {
      ResourceRecordSet<?> oldRecordSet = nextOrNull(iterateByNameAndType(name, type));

      if (oldRecordSet != null) {
        api.deleteResourceRecords(zoneName, oldRecordSet);
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
      api.deleteResourceRecords(zoneName, rrSet);
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
    public VerisignMDNSAllProfileResourceRecordSetApi create(String name) {
      return new VerisignMDNSAllProfileResourceRecordSetApi(api, name);
    }
  }

}
