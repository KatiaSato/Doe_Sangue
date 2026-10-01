# Consulta de restrições para doação

## Confirmado

A usuária aprovou substituir a triagem demonstrativa por listas informativas por
categoria, encerradas com “Para mais informações, contate o hemocentro.”

[Figma aprovado](https://www.figma.com/design/OyI66BkEa9qPpGZ1mNJIWj?node-id=3-76):
menu 3:76; medicamentos 6:25; idade/peso 6:56; doenças/sintomas 6:87;
vacinas/procedimentos 6:118. Conteúdo e inventário em `docs/figma/restricoes/listas.json`.

## Implementado

- Cartão Antes de doar nas duas versões da Home.
- Menu com quatro categorias e listas roláveis conforme o Figma.
- Fontes oficiais abertas no navegador; erro tratado se não houver navegador.
- Texto de contato ao final de todas as listas.
- Rotas tipadas com categoria como argumento; retorno à tela de origem.
- Conteúdo editorial em `presentation/restrictions/RestrictionContent.kt`.
- UI Compose em `ui/screens/restrictions/RestrictionScreens.kt`.
- Fonte Poppins e seta original do Figma, em recurso local de 24 dp.

A barra de status é a do Android. Áreas de toque têm altura mínima acessível,
e textos podem crescer e rolar com a escala de fonte do aparelho.
As demais seções e abas da Home existente foram preservadas; a frase que
afirmava aptidão foi substituída pela orientação para agendar a visita.

## Removido

O antigo catálogo demo, DTO, repository, casos de uso, ViewModel, busca,
calendário, cálculo de prazo fictício, resultados individuais e testes exclusivos.
Recursos de resultados sem uso e dependências exclusivas do calendário também
foram retirados. O guia inicial e os SVGs em documentação são referência histórica,
não a especificação atual. O Figma original não foi alterado.

## Fontes do conteúdo

Consultadas durante o desenho em 29/09/2026:

- [Hemominas — condições e restrições](https://www.hemominas.mg.gov.br/condicoes-e-restricoes).
- [Ministério da Saúde — perguntas frequentes](https://www.gov.br/saude/pt-br/composicao/saes/doacao-de-sangue/faq).
- [Hemocentro Unicamp — critérios](https://www.hemocentro.unicamp.br/perguntas-frequentes/criterios-para-doacao-de-sangue/).

As listas não são exaustivas, não determinam aptidão e não alimentam cálculo.
O aplicativo não recomenda interromper tratamentos. A avaliação é do hemocentro.
O conteúdo é local e deve ser revisado quando as fontes mudarem.

## Pendente

Integração com Supabase e contatos por unidade continuam fora deste recorte.
Não há coleta de informações clínicas pessoais nem cálculo individual.
Resultados das verificações estão em [validação](12-validacao.md).
