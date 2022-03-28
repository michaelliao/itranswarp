-- Request a sharding id
-- usage:
-- KEYS: max-id, clientId, ttl
-- ARGV: None
-- return: 0 ~ max-id, or -1 if no available id.

local KEY_SHARDING_PREFIX = '_Sharding_'
local maxId = tonumber(KEYS[1])
local clientId = KEYS[2]
local ttl = tonumber(KEYS[3])
local key

for i = 0, maxId do
    key = KEY_SHARDING_PREFIX .. i
    if redis.call('SETNX', key, clientId) == 1 then
        redis.call('EXPIRE', key, ttl)
        return i
    end
end

return -1
