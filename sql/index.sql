-- Users should be looked up fast by name
create index chatuser_01 on chatuser (name);
-- Message should be found very fast by user_id
create index privatemessage_01 on privatemessage (recipient_id);
create index privatemessage_02 on privatemessage (sender_id);

-- Alter
ALTER TABLE image DROP column url;
ALTER TABLE image ADD COLUMN data bytea[];