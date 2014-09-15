# Energinie cloud protocol
August 2014 - Sascha (@dersascha)

## Data flow
All traffic goes as UDP from client (energinie box) port 1026 to server (cloud) port 1025.

1. (client) DNS lookup server.energinie.com.
2. (client) Register request package (containing energinie box MAC and local IP).
3. (server) Answer with: Cloud heartbeat (containing client ID and data receiver IP) and register ACK (containing client ID).
4. (client) Data to receiver IP ever 10 seconds (containing client ID, tod (time on device), P, E (as seperated day/single and night field), time since night starts and cloud heartbeat request flag).
5. (server) If last data package contains cloud heartbeat request, send cloud heartbeat (data contains this flag once a minute).

## Wireshark filter
Usage:
`wireshark -Xlua_script:energenie-cloud-dissector.lua energinie.pcap`

## Ruby cloud emulator
* Install required gems: `gem install rubydns`
* Setup local DNS and fake request to `server.energinie.com` or redirect DNS traffic to port 5300: 

```
iptables -t nat -I PREROUTING 1 -i eth0 -p udp --dport 53 -j REDIRECT --to-port 5300
iptables -t nat -I PREROUTING 1 -i eth0 -p udp --dport  -j REDIRECT --to-port 5300
```

* Run server with `ruby energinie.rb`
* (Optional) Enable (uncomment) fetch current data from web page.
