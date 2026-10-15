# Mensagens específicas de autenticação — 15/10/2026

## Estado e escopo

**Git autorizado:** após o fechamento, a usuária autorizou commit/push de campanhas e erros. Referência remota atualizada antes do commit; revisão dos 21 arquivos deste marco sem padrões de segredo ou endereços fora dos domínios de exemplo. Incluir conteúdo completo dos arquivos parcialmente staged. Não pedir autorização novamente; verificar commit/HEAD remoto após envio. Registros de autorização pendente abaixo são históricos. Nenhuma mudança de produção após testes; somente documentação de fechamento atualizada.

**Conferência manual concluída:** capturas confirmam credenciais inválidas → “E-mail ou senha incorretos.” e formato inválido → “Informe um e-mail válido.”, permanecendo no login. Sem registrar endereço/senha nem inspecionar JWT/HTTP. As duas etapas escolhidas para hoje (campanhas e mensagens específicas) estão concluídas nesse recorte. Outros erros remotos foram verificados com fakes/respostas em memória; nenhum novo cadastro/e-mail provocado pelo agente. Commit/push ainda pendentes de autorização; estados anteriores de pendência abaixo são históricos.

**Implementação e validação automatizada concluídas:** usuária escreveu enum/falha/mapper/mensagens e adaptou repository/login/cadastro, incluindo formato do e-mail no login. Classificação aplicada somente em signIn/signUp, preservando cancelamento. Mensagens finais escolhidas pela usuária para duplicidade e senha fraca são, respectivamente, “Já existe uma conta com este e-mail.” e “Escolha uma senha mais forte e confira os requisitos.”; as propostas mais longas abaixo são históricas. Doze testes locais (oito mapper/quatro mensagens) e 20 instrumentados (cinco login/cinco cadastro/dez navegação) aprovados, zero falhas/erros/ignorados nos XML. APK do app/testes e LintDebug aprovados, 31 avisos/zero erros. Repositories/respostas em memória, sem novos pedidos Auth ou envio de e-mails. Próximo passo orientado: conferência visual manual do formato inválido no login; commit/push deste marco ainda não executados. O diagrama abaixo foi proposto antes da implementação e agora descreve o fluxo conectado.

Usuária escolheu concluir campanhas e mensagens específicas hoje, mantendo tutoria. Campanhas concluída no recorte planejado: seed/repetição, leitura/filtros SQL simulados e lista/filtros/detalhe/retorno reais. Conferência final por captura: 2 unidades, 3 horários, 2 perfis, 1 reserva, 0 eventos, 4 campanhas e 2 vínculos; 14 registros. C=NAO_SEI/confirmado/dados=true, 23 agendada/dados=true. Ainda faltam massa de 200 e demais itens acadêmicos.

Implementado antes desta etapa: AuthRepository retorna Result; SupabaseAuthRepository preserva CancellationException, mas devolve demais exceções do SDK sem classificação de domínio. LoginViewModel e RegistrationViewModel usam mensagens genéricas nas falhas remotas; cadastro já diferencia validações locais de nome/e-mail/tipo/senha/confirmação. Login só verifica campos vazios. Telas exibem errorMessage recebido, sem precisar conhecer o Supabase.

Proposto: classificar erros conhecidos de login/cadastro, manter fallback seguro e validar formato do e-mail também no login. Sem alterar sucesso, navegação, senha em memória, contratos Result, logout/restauração, regras do banco ou telas de recuperação/reenvio ainda visuais. Este documento registra o desenho antes da implementação guiada; nenhum arquivo Kotlin criado/editado pelo agente nesta etapa.

## Sequência proposta

```mermaid
sequenceDiagram
    actor Pessoa
    participant Tela as AuthScreens / RegistrationScreens
    participant VM as LoginViewModel / RegistrationViewModel
    participant Repo as SupabaseAuthRepository
    participant Fonte as SupabaseAuthDataSource
    participant Auth as Supabase Auth
    participant Mapper as AuthErrorMapper
    Pessoa->>Tela: Envia login ou cadastro
    Tela->>VM: signIn ou register
    VM->>VM: Valida campos locais
    alt Campo inválido
        VM-->>Tela: Mensagem específica e sem chamada remota
    else Campos válidos
        VM->>Repo: signIn ou signUp
        Repo->>Fonte: Autentica ou cadastra
        Fonte->>Auth: Requisição pelo SDK
        Auth-->>Fonte: Falha com código estruturado
        Fonte-->>Repo: Exceção do SDK
        Repo->>Mapper: Classifica falha conhecida
        Mapper-->>Repo: AuthFailure com AuthError do domínio
        Repo-->>VM: Result.failure
        VM->>VM: Converte categoria em mensagem em português
        VM-->>Tela: errorMessage e fim do carregamento
        Tela-->>Pessoa: Causa conhecida e próxima ação
    end
    Note over Repo,Mapper: CancellationException continua sendo propagada
    Note over VM,Tela: Falha desconhecida recebe fallback; sem exceção bruta
```

## Participantes e arquivos

| Camada | Arquivos e responsabilidade |
| --- | --- |
| Domínio, proposto | `domain/model/AuthError.kt`: categorias; `domain/model/AuthFailure.kt`: exceção com categoria para manter Result existente, sem dependência de SDK ou texto da UI. |
| Dados, proposto | `data/supabase/mapper/AuthErrorMapper.kt`: códigos/tipos do SDK → categoria; reconhecer falhas de transporte por tipo e preservar cancelamento. |
| Dados, existente a adaptar | `data/repository/SupabaseAuthRepository.kt`: classificar falhas de signIn/signUp. DataSource mantém chamadas SDK atuais; logout/restauração fora deste recorte. |
| Apresentação, proposto | `presentation/auth/AuthErrorMessages.kt`: mensagens conhecidas e fallback por operação, sem SDK. |
| Apresentação, existente a adaptar | `LoginViewModel.kt` e `RegistrationViewModel.kt`: usar classificação nas falhas, validar e-mail localmente no login e preservar estados/sucesso. |
| UI, existente | `ui/screens/auth/AuthScreens.kt` e `RegistrationScreens.kt`: exibir errorMessage; sem capturar exceções de rede ou analisar códigos técnicos. |

## Categorias e verificação

Categorias iniciais: INVALID_EMAIL, EMAIL_NOT_CONFIRMED, INVALID_CREDENTIALS, ACCOUNT_ALREADY_EXISTS, WEAK_PASSWORD, RATE_LIMITED, EMAIL_NOT_AUTHORIZED, NETWORK e UNKNOWN. E-mail inválido pede endereço válido; não confirmado orienta confirmar antes de entrar; credenciais inválidas não distingue existência da conta; senha fraca explica regra; limites orientam aguardar; envio não autorizado informa restrição de envio sem chamar de formato inválido; falha de conexão identificada orienta verificar conexão; desconhecidos usam fallback. Reenvio/recuperação não serão prometidos como ação implementada.

Refinamento solicitado pela usuária antes de implementar o enum: cadastro existente → “Já existe uma conta com este e-mail. Entre na sua conta.”, somente quando o servidor retornar explicitamente código como EmailExists/UserAlreadyExists. Autenticação inválida → “E-mail ou senha incorretos.”, usando INVALID_CREDENTIALS; não separar usuário inexistente de senha errada. Não consultar contas para antecipar esse diagnóstico nem concluir duplicidade a partir de HTTP 400, resultado sem sessão ou resposta de confirmação pendente. ACCOUNT_ALREADY_EXISTS é a categoria adicional orientada para AuthError.kt; integração e testes seguem pendentes.

SDK 3.6.0 inspecionado localmente no sources.jar de auth-kt-android: `AuthRestException.errorCode` é AuthErrorCode opcional e `error` guarda o código desconhecido. Enum inclui EmailAddressInvalid, EmailNotConfirmed, InvalidCredentials, WeakPassword, OverRequestRateLimit, OverEmailSendRateLimit e EmailAddressNotAuthorized. Classificar códigos estruturados, nunca HTTP 400 isolado nem comparação de mensagem em inglês. Conferir tipos reais de transporte antes de orientar o mapper; não tratar qualquer exceção como falha de conexão.

Validação prevista: testes locais focados do mapper e mensagens, preservação de cancelamento, validações sem chamada remota; testes instrumentados de login/cadastro para mensagens distintas, recuperação e sucesso preservado. Testes com fakes/transporte controlado, sem enviar novos e-mails ou criar contas reais para provocar erros. Ampliar regressão somente se mudanças/falhas justificarem.

Progresso guiado: AuthError (incluindo ACCOUNT_ALREADY_EXISTS), AuthFailure(reason) e extensão AuthRestException.toAuthError() conferidos nos arquivos escritos pela usuária. Ainda sem integração ao repository ou mensagens na UI. Transporte inspecionado no sources.jar local: SDK 3.6.0 usa HttpRequestException para falhas ao enviar e preserva HttpRequestTimeoutException/ CancellationException. Próximo trecho orientado: extensão Throwable.toAuthFailure(), reconhecendo esses tipos de transporte, propagando CancellationException e preservando AuthFailure já classificada. Não classificar qualquer Exception/HTTP 400 como falha de conexão. Testes do mapper ainda pendentes.

## Próximo passo em tutoria

Conferências manuais encerradas conforme o estado acima. Próxima operação: executar commit/push já autorizado e verificar sincronização. Nenhuma próxima funcionalidade autorizada automaticamente; manter tutoria e novo diagrama antes de outra etapa. Demais funcionalidades (reenvio/recuperação/Google/visual do Perfil) continuam fora deste recorte.

Histórico de teste: primeira tentativa do mapper falhou por import InternalAPI do teste criado pelo agente; corrigido para io.ktor.utils.io.InternalAPI e execução seguinte aprovada. Nenhum ajuste automático em produção. Respostas HTTP construídas em memória, nenhum pedido remoto/conta/e-mail.
