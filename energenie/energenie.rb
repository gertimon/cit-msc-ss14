#!/usr/bin/ruby
#
# Energinie cloud emulator - Aug. 2014 - Sascha Wolke <sascha.wolke@campus.tu-berlin.de>
#
# iptables -t nat -I PREROUTING 1 -i eth0 -p udp --dport 53 -j REDIRECT --to-port 5300
# iptables -t nat -I PREROUTING 1 -i eth0 -p udp --dport  -j REDIRECT --to-port 5300
#
require 'socket'
require 'rubydns'
require 'net/http'
require 'json'

DUMMY_SERVER = [192, 168, 2, 2] # local IP
DUMMY_DNS_PORT = 5300
CLOUD_ID = [0x12, 0x34, 0x56, 0x78] # box cloud id
REAL_DNS_SERVER = RubyDNS::Resolver.new([[:udp, '192.168.2.1', 53]])

ENERGENIE_BOX = '192.168.2.35'
LOGIN_URI = URI("http://#{ENERGENIE_BOX}/login.html")
ENERGENIE_BOX_PW='1'
DATA_URI = URI("http://#{ENERGENIE_BOX}/energenie.html")

#### DNS ####
dns = Thread.new do
  RubyDNS::run_server(listen: [[:udp, DUMMY_SERVER, DUMMY_DNS_PORT]]) do
    match(/server.energenie.com/, Resolv::DNS::Resource::IN::A) do |transaction|
      transaction.respond!(DUMMY_SERVER)
    end

    otherwise do |transaction|
      transaction.passthrough!(REAL_DNS_SERVER)
    end
  end
end

CLOUD_ACK = ([0x0b] + CLOUD_ID + DUMMY_SERVER).pack('c*')
CLOUD_INIT = ([0x2c, 0x12, 0x34, 0x56, 0x78] + CLOUD_ID + [0x00]).pack('c*')

#### CLOUD EMULATOR ####
# File.open('energy.log', 'a') do |out|
  Socket.udp_server_loop(DUMMY_SERVER, 1025) do |msg, msg_src|
    data = msg.unpack('c*')

    if data[0] == 0x0f
      puts "Hello received, saying hello too"
      msg_src.reply CLOUD_ACK
      msg_src.reply CLOUD_INIT

    elsif data[0] == 0x0d
      puts "Got data=#{msg.unpack("H*")}"

      if data[20] == 1 # heartbeat requested
        puts "Sending requested heartbeat..."
        msg_src.reply CLOUD_ACK
      end

      # fetch current data from web page and log them together
      # web = Net::HTTP.get DATA_URI
      # if web =~ /<script>[^<]+(var *period [^<]* *;)</
      #   out.puts("{\"ts:\": #{Time.now.to_i}, #{$1.gsub(/ *var *(\w+) *= *(\d+) *;/, '"\1": \2, ')} \"cloud\": \"#{msg.unpack("H*")[0]}\"}\" }")
      #   out.flush

      # elsif web =~ /Password/
      #   puts "Can't access web frontend. Running login."
      #   Net::HTTP.post_form(LOGIN_URI, 'pw' => ENERGENIE_BOX_PW)
      # end
    end
  end
# end

dns.kill
