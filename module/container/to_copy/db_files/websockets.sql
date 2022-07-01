/*
 * websockets.sql	websocket database
 */

PRAGMA foreign_keys=OFF;
BEGIN TRANSACTION;

Create Table websockets(
	websocket_id	TEXT PRIMARY KEY,
	is_active		BOOLEAN,
	is_healthy	BOOLEAN,
	last_update INTEGER,
	count_err INTEGER
);



COMMIT;

