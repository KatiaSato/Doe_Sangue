package br.edu.fatec.doesangue.presentation.restrictions

// Conteúdo editorial revisado em 06/10/2026. Referências em docs/16-revisao-regras-doacao.md.
// O layout veio do Figma; os textos atuais foram atualizados somente no Android.
// Não representa regras de aptidão nem substitui a avaliação do hemocentro.
enum class RestrictionCategory { MEDICATIONS, AGE_WEIGHT, HEALTH, PROCEDURES }

data class RestrictionItem(val title: String, val description: String)

data class RestrictionContent(
    val category: RestrictionCategory,
    val title: String,
    val summary: String,
    val introduction: String,
    val items: List<RestrictionItem>,
    val sourceName: String,
    val sourceUrl: String,
)

object DonationRestrictions {
    const val reviewedOn = "06/10/2026"
    private const val regulationUrl =
        "https://bvsms.saude.gov.br/bvs/saudelegis/gm/2026/prt11685_03_07_2026.html"
    private const val regulationName = "Ministério da Saúde — Portaria 11.685/2026"

    val categories: List<RestrictionContent> = listOf(
        RestrictionContent(
            category = RestrictionCategory.MEDICATIONS,
            title = "Uso de medicamentos",
            summary = "Veja orientações sobre medicamentos e cuidados antes de doar.",
            introduction = "Informe os medicamentos em uso. Não interrompa tratamentos para doar.",
            items = listOf(
                RestrictionItem("Antibióticos", "A avaliação considera o medicamento e a infecção que motivou seu uso."),
                RestrictionItem("Isotretinoína (Roacutan)", "Exige um período de espera após a última dose. Confirme com o hemocentro."),
                RestrictionItem("Anticoagulantes", "O tratamento e a doença de base precisam ser avaliados antes da doação."),
                RestrictionItem("Medicamentos de uso contínuo", "Informe nome, dose e motivo do uso, incluindo remédios para pressão e saúde mental."),
                RestrictionItem("PrEP e PEP", "A norma prevê 4 meses após a última dose de PrEP ou PEP oral e 24 meses após a última aplicação de PrEP injetável de longa duração. Não interrompa a prevenção para doar; converse com o hemocentro."),
                RestrictionItem("Injetáveis para diabetes e obesidade (GLP-1)", "Informe o medicamento, mudanças de dose, efeitos adversos e eventual compartilhamento de canetas ou agulhas. A equipe avalia o tratamento e as condições de uso; não suspenda a medicação para doar."),
            ),
            sourceName = regulationName,
            sourceUrl = regulationUrl,
        ),
        RestrictionContent(
            category = RestrictionCategory.AGE_WEIGHT,
            title = "Idade e peso",
            summary = "Conheça os requisitos básicos para a doação.",
            introduction = "Confira os critérios gerais. A avaliação final é feita pelo hemocentro.",
            items = listOf(
                RestrictionItem("Faixa etária", "A faixa geral vai de 16 anos completos a 69 anos, 11 meses e 29 dias. Doadores de repetição podem continuar após os 70 anos, mediante avaliação médica pelo serviço de hemoterapia."),
                RestrictionItem("Primeira doação", "O limite geral é 60 anos, 11 meses e 29 dias. Situações excepcionais dependem de avaliação médica e justificativa do serviço."),
                RestrictionItem("16 e 17 anos", "É necessário consentimento formal do responsável legal."),
                RestrictionItem("Peso mínimo", "O requisito geral é pesar pelo menos 50 kg."),
                RestrictionItem("Documento com foto", "Leve um documento oficial com foto para identificação na unidade."),
            ),
            sourceName = regulationName,
            sourceUrl = regulationUrl,
        ),
        RestrictionContent(
            category = RestrictionCategory.HEALTH,
            title = "Doenças e sintomas",
            summary = "Consulte orientações sobre condições de saúde.",
            introduction = "Relate seu histórico de saúde na triagem. Esta lista não reúne todas as situações.",
            items = listOf(
                RestrictionItem("Gripe, resfriado e febre", "Adie a doação enquanto estiver com sintomas e confirme quando poderá retornar."),
                RestrictionItem("Diabetes e hipertensão", "A equipe avalia o controle da doença, os medicamentos e possíveis complicações."),
                RestrictionItem("Histórico de hepatite", "Informe o tipo de hepatite e quando ocorreu para a avaliação do hemocentro."),
                RestrictionItem("Outras doenças e tratamentos", "Relate diagnósticos e tratamentos atuais ou anteriores, mesmo sem sintomas."),
            ),
            sourceName = "Hemocentro Unicamp",
            sourceUrl = "https://www.hemocentro.unicamp.br/perguntas-frequentes/criterios-para-doacao-de-sangue/",
        ),
        RestrictionContent(
            category = RestrictionCategory.PROCEDURES,
            title = "Vacinas e procedimentos",
            summary = "Veja informações sobre vacinas, tatuagens e cirurgias.",
            introduction = "Informe o que foi realizado e a data. O tempo de espera varia conforme o caso.",
            items = listOf(
                RestrictionItem("Vacinas", "A espera depende da vacina recebida. Informe o nome e a data da aplicação."),
                RestrictionItem("Tatuagem, maquiagem definitiva e estética invasiva", "A espera é de 7 dias quando a segurança sanitária do procedimento pode ser avaliada, ou 4 meses quando não pode. A avaliação cabe ao hemocentro; o prazo não garante aptidão."),
                RestrictionItem("Piercings", "A espera é de 7 dias se a segurança do procedimento puder ser avaliada, ou 4 meses se não puder. Na boca ou região genital, o impedimento permanece enquanto estiver colocado, com espera de 4 meses após a retirada."),
                RestrictionItem("Endoscopia e colonoscopia", "Procedimentos endoscópicos exigem espera de 4 meses após a realização. Informe também o motivo do exame e o resultado à equipe."),
                RestrictionItem("Cirurgias e tratamento dentário", "A avaliação considera o procedimento, a recuperação e os medicamentos usados."),
            ),
            sourceName = "Hemominas — atualização de setembro de 2026",
            sourceUrl = "https://portal.hemominas.mg.gov.br/hemominas-adota-novos-criterios-para-doacao-de-sangue",
        ),
    )

    fun forCategory(category: RestrictionCategory): RestrictionContent =
        categories.first { it.category == category }
}
