package com.aistudio.cleanmind.app.ocr

import com.aistudio.cleanmind.app.domain.model.DocumentClassification
import java.util.Locale

class DocumentClassifier {

    private val invoiceKeywords = setOf(
        "nota fiscal", "danfe", "nf-e", "nfe", "cupom fiscal", "comprovante", "fatura", "recibo",
        "cnpj", "cpf", "valor total", "subtotal", "pagamento", "boleto", "vencimento", "chave de acesso"
    )

    private val contractKeywords = setOf(
        "contrato", "cláusula", "contratante", "contratada", "acordo", "termo de posse",
        "locação", "rescisão", "testemunhas", "assinatura", "foro da comarca"
    )

    private val financialKeywords = setOf(
        "extrato", "banco", "saldo", "conta corrente", "poupança", "lançamentos",
        "crédito", "débito", "investimento", "iof", "tarifa bancária", "transferência"
    )

    private val identityKeywords = setOf(
        "registro geral", "república federativa", "carteira de identidade", "cnh",
        "habilitação", "órgão emissor", "nacionalidade", "filiação", "título de eleitor"
    )

    private val academicKeywords = setOf(
        "certificado", "diploma", "curso", "conclusão", "graduação", "instituição",
        "histórico escolar", "horas complementares", "reconhecido pelo mec"
    )

    fun classifyText(text: String): DocumentClassification {
        if (text.isBlank()) return DocumentClassification.GENERIC_DOCUMENT

        val lower = text.lowercase(Locale.ROOT)

        val invoiceScore = countMatches(lower, invoiceKeywords)
        val contractScore = countMatches(lower, contractKeywords)
        val financialScore = countMatches(lower, financialKeywords)
        val identityScore = countMatches(lower, identityKeywords)
        val academicScore = countMatches(lower, academicKeywords)

        val maxScore = maxOf(invoiceScore, contractScore, financialScore, identityScore, academicScore)
        if (maxScore == 0) return DocumentClassification.GENERIC_DOCUMENT

        return when (maxScore) {
            invoiceScore -> DocumentClassification.INVOICE_RECEIPT
            contractScore -> DocumentClassification.CONTRACT_LEGAL
            financialScore -> DocumentClassification.FINANCIAL_STATEMENT
            identityScore -> DocumentClassification.IDENTITY_DOCUMENT
            academicScore -> DocumentClassification.CERTIFICATE_ACADEMIC
            else -> DocumentClassification.GENERIC_DOCUMENT
        }
    }

    fun extractKeywords(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val lower = text.lowercase(Locale.ROOT)
        val allKeywords = invoiceKeywords + contractKeywords + financialKeywords + identityKeywords + academicKeywords
        return allKeywords.filter { lower.contains(it) }.take(5)
    }

    private fun countMatches(text: String, keywords: Set<String>): Int {
        var count = 0
        for (kw in keywords) {
            if (text.contains(kw)) {
                count++
            }
        }
        return count
    }
}
