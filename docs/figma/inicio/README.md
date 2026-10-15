# Proposta visual da Home — 15/10/2026

Criada a pedido da usuária para avaliar a hierarquia visual após a integração funcional. [Abrir a tela no Figma](https://www.figma.com/design/OyI66BkEa9qPpGZ1mNJIWj?node-id=33-56), página **Proposta — Início**, quadro **Home — proposta de hierarquia / 15.10.2026**. Referência anterior preservada na outra página do mesmo arquivo. IDs e evidência estrutural em `proposta.json`.

- Um único botão de destaque: Agendar doação.
- Próximo agendamento em cartão com data, unidade e acesso discreto aos detalhes; histórico/lista ficam na barra Doações.
- Antes de doar em faixa informativa; estoque em indicadores compactos com nível por texto/cor e unidade explícita.
- Campanhas com ilustração e conteúdo editorial; Ver todos/Ver todas são links secundários.
- Poppins e identidade vermelha preservados; dois assets do aplicativo reutilizados, com dados sintéticos. Cores escuras de ação e estados foram propostas localmente para contraste, sem modificar os tokens anteriores.

Validação: 121 descendentes editáveis (36 frames, 38 textos, 20 instâncias, 26 vetores e um retângulo), fonte Poppins em todos os textos, nenhuma extrapolação de limites. As duas imagens são somente ilustrações isoladas. Captura completa revisada, sem sobreposição ou texto cortado. Botão/indicadores existentes reutilizados; novos ícones vetoriais Lucide 1.8.0/ISC e item de navegação componentizado ficam na área auxiliar.

O quadro mostra o conteúdo inteiro para avaliação. A usuária aprovou a proposta e autorizou sua implementação em Compose em 15/10/2026. Na Home Android, a área central rola e a navegação permanece fixa; dados e destinos existentes são preservados. Ícones originais estão em assets e foram convertidos para VectorDrawable; ilustrações reutilizam os recursos do aplicativo. Desenho, adaptação nativa, arquivos e validação da implementação em [docs/30](../../30-home-design-compose.md). As outras telas e o banco permanecem fora deste redesenho.
