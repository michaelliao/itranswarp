-- Renew a sharding-id
-- usage:
-- KEYS: id, clientId, ttl
-- ARGV: None
-- return: id, or -1 if renew failed.

local KEY_SHARDING_PREFIX = '_Sharding_'
local id = tonumber(KEYS[1])
local key = KEY_SHARDING_PREFIX .. id
local clientId = KEYS[2]
local ttl = tonumber(KEYS[3])

if redis.call('GET', key) == clientId then
    if redis.call('EXPIRE', key, ttl) == 1 then
        return id
    end
end

return -1
