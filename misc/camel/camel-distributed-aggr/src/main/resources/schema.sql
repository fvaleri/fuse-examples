--mysql/derby
create table aggregation
(
    id varchar(255) not null primary key,
    exchange blob not null,
    version bigint not null
);

create table aggregation_completed
(
    id varchar(255) not null primary key,
    exchange blob not null,
    version bigint not null
);

--postgres
create table aggregation
(
    id varchar(255) not null primary key,
    exchange bytea not null,
    version bigint not null
);

create table aggregation_completed
(
    id varchar(255) not null primary key,
    exchange bytea not null,
    version bigint not null
);
