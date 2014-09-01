-- Energinie cloud protocol dissector
-- Run with: wireshark -Xlua_script:energenie-cloud-dissector.lua
-- 
-- Aug. 2014 - Sascha Wolke <sascha.wolke@campus.tu-berlin.de>

-- declare our protocol
energenie_proto = Proto("energenie","Energenie Cloud Protocol")

-- create a function to dissect it
function energenie_proto.dissector(buffer,pinfo,tree)
    pinfo.cols.protocol = "Energenie"
    local cmd = buffer(0,1):uint()

    -- client register
    if cmd == 0x0f then
        local subtree = tree:add(energenie_proto,buffer(),"Energenie Cloud Protocol (Register request)")
        subtree:add(buffer(1,6), "Powermeter MAC: " .. tostring(buffer(1,6):ether()))
        subtree:add(buffer(7,4), "Powermeter IP: " .. tostring(buffer(7,4):ipv4()))

    -- cloud ack/heartbeat
    elseif cmd == 0x0b then
        local subtree = tree:add(energenie_proto,buffer(),"Energenie Cloud Protocol (Cloud heartbeat)")
        subtree:add(buffer(1,4), "Cloud ID: " .. buffer(1,4))
        subtree:add(buffer(5,4), "Receiver IP: " .. tostring(buffer(5,4):ipv4()))

    -- cloud register ack?
    elseif cmd == 0x2c then
        local subtree = tree:add(energenie_proto,buffer(),"Energenie Cloud Protocol (Cloud register ack?)")
        subtree:add(buffer(5,4), "Cloud ID: " .. buffer(5,4))

    -- client data
    elseif cmd == 0x0d then
        local subtree = tree:add(energenie_proto,buffer(),"Energenie Cloud Protocol (Client data)")
        subtree:add(buffer(2,4), "Cloud ID: " .. buffer(2,4))
        subtree:add(buffer(8,4), "Time on device (tod): "
            .. format_date(buffer(8,4):le_uint()))
        subtree:add(buffer(12,2), "P in W: "
            .. (buffer(12,2):uint() * 2 ^ 8) / 466.0)
        subtree:add(buffer(16,4), "E in kWh (day/single): "
            .. buffer(16,4):uint() / (0xffff * 100.0)) -- last bytes is .XXXX value => / 25600 
        subtree:add(buffer(20,1), "Cloud heartbeet requested: "
            .. tostring(buffer(20,1):uint() == 1))
        subtree:add(buffer(23,4), "E in kWh (night): "
            .. buffer(23,4):uint() / (0xffff * 100.0))
        subtree:add(buffer(23,4), "E in kWh (day+night): "
            .. (buffer(16,4):uint() + buffer(23,4):uint()) / (0xffff * 100.0))
        subtree:add(buffer(27,1), "Day/Night mode: "
            .. tostring(buffer(27,1):uint() == 1))
        subtree:add(buffer(28,2), "Time since night start: "
            .. format_time(buffer(28,2):le_uint() * 2))
        subtree:add(buffer(30,2), "Day/Night Settings: "
            .. buffer(30,2))
        subtree:add(buffer(32,1), "Settings changed??? "
            .. buffer(32,1))

    -- unknown
    else
        local subtree = tree:add(energenie_proto,buffer(),"Energenie Cloud Protocol (Unknown command)")
        subtree:add(buffer(0,1),"The first command byte: " .. buffer(0,1))

    end
end

-- load the udp.port table
udp_table = DissectorTable.get("udp.port")
udp_table:add(1025, energenie_proto)
