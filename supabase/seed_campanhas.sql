-- Campanhas sintéticas para a demonstração acadêmica.
begin;

lock table public.campanha,
           public.campanha_tipo_sanguineo
in share row exclusive mode;

do $$
declare
    v_unidade_a bigint;
    v_unidade_b bigint;
        v_fonte constant text :=
            'Demonstração acadêmica Doe Sangue — campanhas v1';

        v_inicio_local constant timestamp :=
            timestamp '2026-10-15 00:00:00';

        v_fim_local constant timestamp :=
            timestamp '2026-12-01 00:00:00';
begin
    select id into strict v_unidade_a
    from public.unidade_coleta
    where nome = 'Unidade Demo A'
      and cidade = 'Cidade fictícia'
      and endereco = 'Endereço fictício A'
      and fuso_horario = 'America/Sao_Paulo'
      and publicada = true
    for share;

    select id into strict v_unidade_b
    from public.unidade_coleta
    where nome = 'Unidade Demo B'
      and cidade = 'Cidade fictícia'
      and endereco = 'Endereço fictício B'
      and fuso_horario = 'America/Sao_Paulo'
      and publicada = false
    for share;

        insert into public.campanha (
            unidade_id, titulo, descricao, inicio, fim, fonte, publicada
        )
        select
            demo.unidade_id,
            demo.titulo,
            demo.descricao,
            v_inicio_local at time zone unidade.fuso_horario,
            v_fim_local at time zone unidade.fuso_horario,
            v_fonte,
            demo.publicada
                from (
                    values
                        (
                            v_unidade_a,
                            'Campanha Demo A — incentivo à doação',
                            'Divulgação fictícia para a demonstração acadêmica.',
                            true
                        ),
                        (
                            v_unidade_a,
                            'Campanha Demo A — tipos A+ e O+',
                            'Campanha fictícia destinada aos tipos A+ e O+.',
                            true
                        ),
                        (
                            v_unidade_a,
                            'Campanha Demo A — rascunho',
                            'Rascunho fictício que não deve aparecer no aplicativo.',
                            false
                        ),
                        (
                             v_unidade_b,
                             'Campanha Demo B — divulgação',
                             'Campanha fictícia de uma unidade não publicada.',
                             true
                         )

                ) as demo(unidade_id, titulo, descricao, publicada)
        join public.unidade_coleta as unidade
            on unidade.id = demo.unidade_id
        where not exists (
            select 1
            from public.campanha as existente
            where existente.unidade_id = demo.unidade_id
              and existente.titulo = demo.titulo
              and existente.fonte = v_fonte
        );
            insert into public.campanha_tipo_sanguineo (
                campanha_id, tipo_sanguineo
            )
            select campanha.id, tipo.valor
            from public.campanha as campanha
            cross join (
                values
                    ('A+'::public.tipo_sanguineo),
                    ('O+'::public.tipo_sanguineo)
            ) as tipo(valor)
            where campanha.unidade_id = v_unidade_a
              and campanha.titulo = 'Campanha Demo A — tipos A+ e O+'
              and campanha.fonte = v_fonte
            on conflict (campanha_id, tipo_sanguineo) do nothing;

                if (
                    select count(*)
                    from public.campanha
                    where fonte = v_fonte
                ) <> 4 then
                    raise exception
                        'Seed de campanhas: eram esperadas exatamente quatro campanhas.';
                end if;

                    if exists (
                        select 1
                        from public.campanha as campanha
                        join public.unidade_coleta as unidade
                            on unidade.id = campanha.unidade_id
                        where campanha.fonte = v_fonte
                          and (
                              campanha.inicio <> (
                                  v_inicio_local at time zone unidade.fuso_horario
                              )
                              or campanha.fim <> (
                                  v_fim_local at time zone unidade.fuso_horario
                              )
                              or campanha.publicada <> (
                                  campanha.titulo <> 'Campanha Demo A — rascunho'
                              )
                          )
                    ) then
                        raise exception
                            'Seed de campanhas: período ou publicação diferente do esperado.';
                    end if;

                        if (
                            select
                                count(*) <> 2
                                or count(*) filter (
                                    where campanha.unidade_id = v_unidade_a
                                      and campanha.titulo = 'Campanha Demo A — tipos A+ e O+'
                                      and vinculo.tipo_sanguineo in ('A+', 'O+')
                                ) <> 2
                            from public.campanha_tipo_sanguineo as vinculo
                            join public.campanha as campanha
                                on campanha.id = vinculo.campanha_id
                            where campanha.fonte = v_fonte
                        ) then
                            raise exception
                                'Seed de campanhas: vínculos de tipos diferentes do esperado.';
                        end if;
end;
$$;

commit;