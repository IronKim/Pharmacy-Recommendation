package com.fs.pharmacyrecommendation.api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fs.pharmacyrecommendation.AbstractIntegrationContainerBaseTest
import com.fs.pharmacyrecommendation.api.dto.DocumentDto
import com.fs.pharmacyrecommendation.api.dto.KakaoApiResponseDto
import com.fs.pharmacyrecommendation.api.dto.MetaDto
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType


class KakaoAddressSearchServiceRetryTest extends AbstractIntegrationContainerBaseTest {

    @Autowired
    private KakaoAddressSearchService kakaoAddressSearchService

    @SpringBean // 스프링 빈을 주입받을 수 있다.
    private KakaoUriBuilderService kakaoUriBuilderService = Mock() // Mock 객체를 주입받을 수 있다.

    private MockWebServer mockWebServer // 서버를 가짜로 구동할 수 있다.

    private ObjectMapper mapper = new ObjectMapper()

    private String inputAddress = "서울 성북구 종암로 10길"

    def setup() {
        mockWebServer = new MockWebServer()
        mockWebServer.start() // 서버를 가동한다.
        println mockWebServer.port // 서버의 포트를 확인할 수 있다.
        println mockWebServer.url("/") // 서버의 url을 확인할 수 있다.
    }

    def cleanup() {
        mockWebServer.shutdown() // 서버를 종료한다.
    }

    def "requestAddressSearch retry success"() {
        given:
        def metaDto = new MetaDto(1)
        def documentDto = DocumentDto.builder()
                .addressName(inputAddress)
                .build()
        def expectedResponse = new KakaoApiResponseDto(metaDto, Arrays.asList(documentDto))
        def uri = mockWebServer.url("/").uri()

        when:
        mockWebServer.enqueue(new MockResponse().setResponseCode(504)) // 504 에러를 발생시킨다.
        mockWebServer.enqueue(new MockResponse().setResponseCode(200) // 200 성공을 발생시킨다.
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mapper.writeValueAsString(expectedResponse)))

        def kakaoApiResult = kakaoAddressSearchService.requestAddressSearch(inputAddress)

        then:
        2 * kakaoUriBuilderService.buildUriByAddressSearch(inputAddress) >> uri // 2번 호출되었는지 확인한다.
        kakaoApiResult.getDocumentList().size() == 1
        kakaoApiResult.getMetaDto().totalCount == 1
        kakaoApiResult.getDocumentList().get(0).getAddressName() == inputAddress

    }


    def "requestAddressSearch retry fail "() {
        given:
        def uri = mockWebServer.url("/").uri()

        when:
        mockWebServer.enqueue(new MockResponse().setResponseCode(504))
        mockWebServer.enqueue(new MockResponse().setResponseCode(504))

        def result = kakaoAddressSearchService.requestAddressSearch(inputAddress)

        then:
        2 * kakaoUriBuilderService.buildUriByAddressSearch(inputAddress) >> uri
        result == null
    }
}