@file:OptIn(io.github.jan.supabase.annotations.SupabaseInternal::class)
package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.data.repository.*
import br.edu.fatec.doesangue.domain.model.*
import io.github.jan.supabase.*
import io.github.jan.supabase.auth.*
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.*
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.*
import org.junit.Assert.*
import java.time.LocalDate

/** Transporte totalmente interceptado, credenciais e respostas sintéticas. */
class RescheduleRoutineSdkTest {
    private lateinit var client: SupabaseClient
    private val a="00000000-0000-4000-8000-000000000001"
    private val b="00000000-0000-4000-8000-000000000002"
    private val key="00000000-0000-4000-8000-000000000099"
    private val requests=mutableListOf<Triple<String,String?,JsonObject>>()
    private var code=200
    private var detail: String?=null
    private var swap=false
    private var wrongOwner=false
    private val choice=RoutineChoice(LocalDate.of(2026,11,20),21,"America/Sao_Paulo",0)
    private fun appointment()="""{"id":91,"doador_id":"$a","horario_id":7,"estado":"agendado","chave_idempotencia":"$key","criado_em":"2026-10-26T12:00:00Z"}"""
    private fun routine()="""{"id":4,"doador_id":"${if(wrongOwner)b else a}","inicio":"2026-11-20","intervalo_dias":21,"fuso_horario":"America/Sao_Paulo","ativa":true,"versao":9,"agendamento_origem_id":91}"""
    @Before fun setup() {
        client=createSupabaseClient("https://f13-f14.example.test","sb_publishable_synthetic") {
            install(Auth) {sessionManager=MemorySessionManager();codeVerifierCache=MemoryCodeVerifierCache()
                autoLoadFromStorage=false;autoSaveToStorage=false;alwaysAutoRefresh=false;enableLifecycleCallbacks=false}
            install(Postgrest)
            httpEngine=OkHttp.create {config {addInterceptor {chain ->
                val req=chain.request();val login=req.url.encodedPath.startsWith("/auth/")
                val body=if(login) """{"access_token":"synthetic-a","refresh_token":"synthetic-refresh","token_type":"bearer","expires_in":3600,"user":{"id":"$a","aud":"authenticated","role":"authenticated","created_at":"2026-10-26T00:00:00Z","app_metadata":{},"user_metadata":{}}}"""
                else {
                    val endpoint=req.url.encodedPath.substringAfterLast('/')
                    assertEquals("public",req.header("Content-Profile"))
                    val buffer=Buffer();req.body?.writeTo(buffer)
                    requests+=Triple(endpoint,req.header("Authorization"),Json.parseToJsonElement(buffer.readUtf8()).jsonObject)
                    if(swap) {
                        val session=checkNotNull(client.auth.currentSessionOrNull())
                        client.auth.setSessionStatus(SessionStatus.Authenticated(session.copy(accessToken="synthetic-b",user=session.user!!.copy(id=b))))
                    }
                    when {
                        code!=200 -> """{"code":"P0001","message":"private synthetic text","details":${detail?.let {"\"$it\""} ?: "null"},"hint":null}"""
                        endpoint=="excluir_minha_rotina" -> "true"
                        endpoint=="consultar_meus_agendamentos_v2" -> """[{"agendamento":${appointment()},"nome_unidade":"Unidade Sintética","inicio":"2026-11-20T11:00:00Z","fuso_horario":"America/Sao_Paulo","unidade_id":2,"remarcado_para_id":92,"remarcado_de_id":23}]"""
                        endpoint.contains("rotina") && endpoint!="criar_agendamento_com_rotina" -> "[${routine()}]"
                        else -> "[${appointment()}]"
                    }
                }
                Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(if(login)200 else code).message("Synthetic")
                    .body(body.toResponseBody("application/json".toMediaType())).build()
            }}}
        }
    }
    @After fun close()=runBlocking {client.close()}
    private suspend fun login() {client.auth.signInWith(Email) {email="synthetic@example.test";password="Synthetic-only-1!"}}
    private fun appointments()=SupabaseAppointmentRepository(SupabaseAppointmentDataSource(client))
    private fun routines()=SupabaseReminderRoutineRepository(SupabaseReminderRoutineDataSource(client))
    @Test fun rescheduleBindsSourceDestinationAndSameKeyToCapturedOwner()=runBlocking<Unit> {
        login();swap=true
        val row=appointments().rescheduleForOwner(a,"23","7",key).getOrThrow()
        assertEquals("91",row.id)
        assertEquals("remarcar_agendamento",requests.single().first)
        assertEquals("Bearer synthetic-a",requests.single().second)
        assertEquals(JsonPrimitive(23),requests.single().third["p_agendamento_id"])
        assertEquals(JsonPrimitive(key),requests.single().third["p_chave_idempotencia"])
        assertTrue(appointments().rescheduleForOwner(a,"23","7",key).isFailure)
        assertEquals(1,requests.size)
    }
    @Test fun creationWithRoutineSendsWholeFingerprintAndMapsDefiniteConflict()=runBlocking<Unit> {
        login();code=400;detail="doesangue.routine_changed.v1"
        repeat(2) {assertEquals(DataError.ROUTINE_CHANGED,(appointments().createWithRoutineForOwner(a,"7",key,choice).exceptionOrNull() as DataFailure).reason)}
        assertEquals(requests[0].third,requests[1].third)
        assertEquals(JsonPrimitive("2026-11-20"),requests[0].third["p_inicio"])
        assertEquals(JsonPrimitive(21),requests[0].third["p_intervalo"])
        assertEquals(JsonPrimitive(0L),requests[0].third["p_versao"])
    }
    @Test fun routineReadSavePauseAndDeleteUseOnlyExpectedOwnerAndStructuredParameters()=runBlocking<Unit> {
        login()
        assertEquals(9L,routines().get(a).getOrThrow()!!.version)
        assertEquals("91",routines().save(a,choice).getOrThrow().originAppointmentId)
        routines().setActive(a,false,9).getOrThrow();routines().delete(a,9).getOrThrow()
        assertEquals(listOf("consultar_minha_rotina","salvar_minha_rotina","definir_minha_rotina_ativa","excluir_minha_rotina"),requests.map {it.first})
        assertEquals(JsonPrimitive(false),requests[2].third["p_ativa"])
        assertTrue(requests.all {it.second=="Bearer synthetic-a"})
        assertTrue(requests.none {it.third.containsKey("doador_id")})
    }
    @Test fun privateRoutineCannotPublishForeignPayloadAndWrongAccountNeverSends()=runBlocking<Unit> {
        login();assertTrue(routines().get(b).isFailure);assertTrue(requests.isEmpty())
        wrongOwner=true;assertTrue(routines().get(a).isFailure)
    }
    @Test fun absentSessionAndInvalidIdentifiersDoNotReachRest()=runBlocking<Unit> {
        assertTrue(routines().get(a).isFailure)
        assertTrue(appointments().rescheduleForOwner(a,"0","7",key).isFailure)
        assertTrue(appointments().createWithRoutineForOwner(a,"-1",key,choice).isFailure)
        assertTrue(requests.isEmpty())
    }
    @Test fun privateSummaryReadsV2AndPreservesBothReschedulingLinks()=runBlocking<Unit> {
        login();val row=appointments().getMyAppointments().getOrThrow().single()
        assertEquals("2",row.centerId);assertEquals("92",row.rescheduledToId);assertEquals("23",row.rescheduledFromId)
        assertEquals("consultar_meus_agendamentos_v2",requests.single().first)
    }
    @Test fun unknownHttpErrorDoesNotInventCurrentStateOrExposeBackendText()=runBlocking<Unit> {
        login();code=400;detail="doesangue.rescheduling_closed.v99"
        assertEquals(DataError.UNKNOWN,(appointments().rescheduleForOwner(a,"23","7",key).exceptionOrNull() as DataFailure).reason)
        detail="doesangue.rescheduling_closed.v1"
        assertEquals(DataError.RESCHEDULING_CLOSED,(appointments().rescheduleForOwner(a,"23","7",key).exceptionOrNull() as DataFailure).reason)
    }
}
