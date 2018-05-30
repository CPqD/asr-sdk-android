package br.com.cpqd.asr.recognizer;

public class TestConstants {

    /**
     * URL Oficial de acesso ao ASR para Testes dos SDKs
     */
    public static final String ASR_URL = "wss://speech.cpqd.com.br/asr/ws/estevan/recognize/8k";

    /**
     * Usuário de acesso à URL Oficial de Testes
     */
    public static final String ASR_User = "estevan";

    /**
     * Senha equivalente ao usuário "ASR_User"
     */
    public static final String ASR_Pass = "Thect195";

    /**
     * URL interna de acesso ao ASR para testes
     */
    public static final String ASR_URL_Internal = "ws://vmh123.cpqd.com.br:8025/asr-server/asr";

    /**
     * Modelo de Fala Livre
     */
    public static final String FreeLanguageModel = "builtin:slm/general";

    /**
     * Modelo de CPF
     */
    public static final String CpfLanguageModel = "builtin:grammar/cpf";

    /**
     * Modelo de Numeros
     */
    public static final String NumberLanguageModel = "builtin:grammar/number";

    /**
     * Áudio com silêncio absoluto
     */
    public static final String SilenceAudio = "silence-8k.wav";

    /**
     * Áudio com multiplos segmentos
     */
    public static final String ContinuosModeAudio = "joao_mineiro_marciano_intro_8k.wav";

    /**
     * Texto do primeiro segmento do audio continuous mode
     */
    public static final String ContinuousModeTextSeg1 = "você me pede na carta que diz apareça";

    /**
     * Texto do segundo segmento do audio continuous mode
     */
    public static final String ContinuousModeTextSeg2 = "que eu nunca mais te procure para sempre te esqueça";

    /**
     * Texto do terceiro segmento do audio continuous mode
     */
    public static final String ContinuousModeTextSeg3 = "posso fazer essa vontade de atender seu pedido";

    /**
     * Gramática de CPF via HTTP
     */
    public static final String CpfGramHttp = "http://vmh102.cpqd.com.br:8280/asr_dist/repository/grammars/dynamic-gram/cpf.gram";

    /**
     * Áudio simples de CPF
     */
    public static final String CpfAudio = "cpf_8k.wav";

    /**
     * Texto equivalente ao áudio "CpfAudio"
     */
    public static final String CpfText = "sete dois oito nove três quatro dois cinco um traço onze";

    /**
     * Interpretação equivalente ao áudio "CpfAudio"
     */
    public static final String CpfInterp = "72893425111";

    /**
     * Gramática de Pizza via HTTP
     */
    public static final String PizzaGramHttp = "http://vmh102.cpqd.com.br:8280/asr_dist/repository/grammars/dynamic-gram/pizza.gram";

    /**
     * Áudio simples de pizza
     */
    public static final String PizzaVegAudio = "pizza_veg_audio_8k.wav";

    /**
     * Texto equivalente ao áudio "PizzaVegAudio"
     */
    public static final String PizzaVegText = "eu quero uma pizza vegetariana";

    /**
     * Interpretação equivalente ao áudio "PizzaVegAudio"
     */
    public static final String PizzaVegInterp = "pizza_vegetariana";

    /**
     * Gramática de Banco Inline
     */
    public static final String BankGramBody = "bank.gram";

    /**
     * Áudio simples de banco
     */
    public static final String BancoTransfiraAudio = "bank_transfira_8k.wav";

    /**
     * Texto equivalente ao áudio "BancoTransfiraAudio
     */
    public static final String BancoTransfiraText = "transfira seis mil novecentos e trinta e sete reais para a conta corrente";

    /**
     * Áudio simples de segmentos hetero
     */
    public static final String HeteroSegAudio = "hetero_segments_8k.wav";

}
