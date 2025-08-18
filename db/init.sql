-- Create DBs and roles for services
CREATE USER users WITH PASSWORD 'users';
CREATE DATABASE usersdb OWNER users;
GRANT ALL PRIVILEGES ON DATABASE usersdb TO users;

CREATE USER progress WITH PASSWORD 'progress';
CREATE DATABASE progressdb OWNER progress;
GRANT ALL PRIVILEGES ON DATABASE progressdb TO progress;
