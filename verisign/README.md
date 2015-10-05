## Notable Behaviors
The following are notable when compared to different providers.
* `Zone.id()` is the `Zone.name()`
* Zone lists are 1 + N requests in order to zip with the SOA's ttl and rname.
* `Zone.ttl()` is the default for new records.
* The zone's NS record set must contain at least 2 nsdnames.
