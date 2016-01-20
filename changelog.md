= Release 0.2.0 =
Released: 2016-01-20

== Features ==
- MQTT Backbone-Protocol first implementation: Now MQTT tunnelling is available
- GC now use system repository as default
- Configuration files in runtime is now possible 

== Bugs ==
- Double MQTT broadcast: GC was re-broadcasting messages arriving into the broadcast topic
- No connection establishment status: GC was not aware if it the Super Node was reached or not
- No connection lost status: GC was not aware if it the Super Node connection was lost or still active
- No connection lost status for messages: GC didn't know if a message was sent or lost because there was no connection to the Super Node
