begin;

alter table public.unidade_coleta
    add column latitude double precision,
    add column longitude double precision,

    add constraint unidade_coleta_coordenadas_par_check
        check (
            (latitude is null and longitude is null)
            or
            (latitude is not null and longitude is not null)
        ),

    add constraint unidade_coleta_latitude_check
        check (latitude between -90 and 90),

    add constraint unidade_coleta_longitude_check
        check (longitude between -180 and 180);

commit;