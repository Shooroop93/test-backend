create table author (
    id          serial primary key,
    fio         varchar not null,
    create_date timestamp default now()
);