package com.aistudio.cleanmind.app.ocr

import com.aistudio.cleanmind.app.domain.model.DocumentClassification
import com.aistudio.cleanmind.app.domain.model.DocumentOcrResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrClassifierTest {

    private val classifier = DocumentClassifier()

    @Test
    fun classifyText_withInvoiceKeywords_returnsInvoiceReceipt() {
        val text = "COMPROVANTE DE PAGAMENTO NOTA FISCAL VALOR TOTAL R$ 150,00 CNPJ 00.000.000/0001-91"
        val classification = classifier.classifyText(text)
        assertEquals(DocumentClassification.INVOICE_RECEIPT, classification)
    }

    @Test
    fun classifyText_withContractKeywords_returnsContractLegal() {
        val text = "CONTRATO DE PRESTAÇÃO DE SERVIÇOS CLÁUSULA PRIMEIRA DAS OBRIGAÇÕES ASSINATURA"
        val classification = classifier.classifyText(text)
        assertEquals(DocumentClassification.CONTRACT_LEGAL, classification)
    }

    @Test
    fun classifyText_withFinancialStatementKeywords_returnsFinancialStatement() {
        val text = "EXTRATO BANCÁRIO CONTA CORRENTE SALDO ANTERIOR LANÇAMENTOS DO MÊS BANCO"
        val classification = classifier.classifyText(text)
        assertEquals(DocumentClassification.FINANCIAL_STATEMENT, classification)
    }

    @Test
    fun classifyText_withIdentityKeywords_returnsIdentityDocument() {
        val text = "REPÚBLICA FEDERATIVA DO BRASIL REGISTRO GERAL CARTEIRA DE IDENTIDADE CPF ÓRGÃO EMISSOR"
        val classification = classifier.classifyText(text)
        assertEquals(DocumentClassification.IDENTITY_DOCUMENT, classification)
    }

    @Test
    fun classifyText_withAcademicKeywords_returnsCertificateAcademic() {
        val text = "CERTIFICADO DIPLOMA DE CONCLUSÃO DO CURSO DE GRADUAÇÃO UNIVERSIDADE"
        val classification = classifier.classifyText(text)
        assertEquals(DocumentClassification.CERTIFICATE_ACADEMIC, classification)
    }

    @Test
    fun classifyText_withGenericOrEmptyText_returnsGenericDocument() {
        val emptyResult = classifier.classifyText("")
        assertEquals(DocumentClassification.GENERIC_DOCUMENT, emptyResult)

        val randomTextResult = classifier.classifyText("Olá mundo, este é um arquivo de texto qualquer")
        assertEquals(DocumentClassification.GENERIC_DOCUMENT, randomTextResult)
    }

    @Test
    fun documentOcrResult_hasText_returnsTrueWhenNonBlank() {
        val result = DocumentOcrResult(
            extractedText = "Nota Fiscal Eletrônica",
            classification = DocumentClassification.INVOICE_RECEIPT,
            confidenceScore = 0.95f,
            keywordsFound = listOf("nota fiscal")
        )
        assertTrue(result.hasText)
        assertEquals(1, result.keywordsFound.size)
    }
}
