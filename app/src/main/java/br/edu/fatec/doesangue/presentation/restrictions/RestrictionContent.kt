package br.edu.fatec.doesangue.presentation.restrictions

// Conteúdo editorial aprovado no Figma, com fontes consultadas em 29/09/2026.
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
            ),
            sourceName = "Hemominas",
            sourceUrl = "https://www.hemominas.mg.gov.br/condicoes-e-restricoes",
        ),
        RestrictionContent(
            category = RestrictionCategory.AGE_WEIGHT,
            title = "Idade e peso",
            summary = "Conheça os requisitos básicos para a doação.",
            introduction = "Confira os critérios gerais. A avaliação final é feita pelo hemocentro.",
            items = listOf(
                RestrictionItem("Faixa etária", "Em geral, de 16 a 69 anos. A primeira doação deve ter ocorrido até os 60 anos."),
                RestrictionItem("16 e 17 anos", "É necessário consentimento formal do responsável legal."),
                RestrictionItem("Peso mínimo", "O requisito geral é pesar pelo menos 50 kg."),
                RestrictionItem("Documento com foto", "Leve um documento oficial com foto para identificação na unidade."),
            ),
            sourceName = "Ministério da Saúde",
            sourceUrl = "https://www.gov.br/saude/pt-br/composicao/saes/doacao-de-sangue/faq",
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
                RestrictionItem("Tatuagem e maquiagem definitiva", "Podem exigir adiamento. A equipe avalia a data e as condições do procedimento."),
                RestrictionItem("Piercings", "Informe a região do corpo e a data da colocação para avaliação."),
                RestrictionItem("Cirurgias e tratamento dentário", "A avaliação considera o procedimento, a recuperação e os medicamentos usados."),
            ),
            sourceName = "Hemocentro Unicamp",
            sourceUrl = "https://www.hemocentro.unicamp.br/perguntas-frequentes/criterios-para-doacao-de-sangue/",
        ),
    )

    fun forCategory(category: RestrictionCategory): RestrictionContent =
        categories.first { it.category == category }
}
