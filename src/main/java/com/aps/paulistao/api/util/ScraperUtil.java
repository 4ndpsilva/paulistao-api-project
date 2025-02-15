package com.aps.paulistao.api.util;

import com.aps.paulistao.api.dto.PartidaGoogleDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.aps.paulistao.api.util.Constants.BASE_URL;
import static com.aps.paulistao.api.util.Constants.COMPLEMENTO_URL;
import static com.aps.paulistao.api.util.Constants.DIV_GOLS_MANDANTE;
import static com.aps.paulistao.api.util.Constants.DIV_GOLS_VISITANTE;
import static com.aps.paulistao.api.util.Constants.DIV_INFO_MANDANTE;
import static com.aps.paulistao.api.util.Constants.DIV_INFO_VISITANTE;
import static com.aps.paulistao.api.util.Constants.DIV_ITEM_GOLS;
import static com.aps.paulistao.api.util.Constants.DIV_JOGO_ROLANDO;
import static com.aps.paulistao.api.util.Constants.DIV_PARTIDA_ENCERRADA;
import static com.aps.paulistao.api.util.Constants.DIV_PARTIDA_NAO_INICIADA;
import static com.aps.paulistao.api.util.Constants.DIV_PENALIDADES;
import static com.aps.paulistao.api.util.Constants.DIV_PLACAR_MANDANTE;
import static com.aps.paulistao.api.util.Constants.DIV_PLACAR_VISITANTE;
import static com.aps.paulistao.api.util.Constants.IMG_LOGO;
import static com.aps.paulistao.api.util.Constants.MANDANTE;
import static com.aps.paulistao.api.util.Constants.PENALTIS;
import static com.aps.paulistao.api.util.Constants.SPAN;
import static com.aps.paulistao.api.util.Constants.SRC;
import static com.aps.paulistao.api.util.Constants.VISITANTE;

@Service
public class ScraperUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScraperUtil.class);

    public PartidaGoogleDTO getInfoPartida(final String url) {
        final PartidaGoogleDTO partidaDTO = new PartidaGoogleDTO();

        try {
            LOGGER.info(url);

            final Document document = Jsoup.connect(url).get();

            final String title = document.title();
            LOGGER.info(title);

            final StatusPartida statusPartida = getStatusPartida(document);
            partidaDTO.setStatusPartida(statusPartida);
            LOGGER.info("Status da Partida: " + statusPartida);

            if (statusPartida != StatusPartida.PARTIDA_NAO_INICIADA) {
                final String tempoPartida = getTempoPartida(document);
                LOGGER.info("Tempo da Partida: " + tempoPartida);
                partidaDTO.setTempoPartida(tempoPartida);

                final Integer placarEquipeMandante = getPlacarEquipe(document, DIV_PLACAR_MANDANTE);
                LOGGER.info("Placar da Equipe Mandante: " + placarEquipeMandante);
                partidaDTO.setPlacarEquipeMandante(placarEquipeMandante);

                final Integer placarEquipeVisitante = getPlacarEquipe(document, DIV_PLACAR_VISITANTE);
                LOGGER.info("Placar da Equipe Visitante: " + placarEquipeVisitante);
                partidaDTO.setPlacarEquipeVisitante(placarEquipeVisitante);

                final String golsEquipeMandante = getGolsEquipe(document, DIV_GOLS_MANDANTE);
                LOGGER.info("Gols da Equipe Mandante: " + golsEquipeMandante);
                partidaDTO.setGolsEquipeMandante(golsEquipeMandante);

                final String golsEquipeVisitante = getGolsEquipe(document, DIV_GOLS_VISITANTE);
                LOGGER.info("Gols da Equipe Visitante: " + golsEquipeVisitante);
                partidaDTO.setGolsEquipeVisitante(golsEquipeVisitante);
            }

            final String nomeEquipeMandante = getNomeEquipe(document, DIV_INFO_MANDANTE);
            LOGGER.info("Equipe Mandante: " + nomeEquipeMandante);
            partidaDTO.setNomeEquipeMandante(nomeEquipeMandante);

            final String nomeEquipeVisitante = getNomeEquipe(document, DIV_INFO_VISITANTE);
            LOGGER.info("Equipe Visitante: " + nomeEquipeVisitante);
            partidaDTO.setNomeEquipeVisitante(nomeEquipeVisitante);

            final String urlLogoEquipeMandante = getUrlLogoEquipe(document, DIV_INFO_MANDANTE);
            LOGGER.info("Logo Equipe Mandante: " + urlLogoEquipeMandante);
            partidaDTO.setLogoEquipeMandante(urlLogoEquipeMandante);

            final String urlLogoEquipeVisitante = getUrlLogoEquipe(document, DIV_INFO_VISITANTE);
            LOGGER.info("Logo Equipe Visitante: " + urlLogoEquipeVisitante);
            partidaDTO.setLogoEquipeVisitante(urlLogoEquipeVisitante);

            final Integer placarEstendidoEquipeMandante = getInfoPenalidades(document, MANDANTE);
            LOGGER.info("Placar de Pênaltis do Mandante: " + placarEstendidoEquipeMandante);
            partidaDTO.setPlacarPenaltisMandante(placarEstendidoEquipeMandante);

            final Integer placarEstendidoEquipeVisitante = getInfoPenalidades(document, VISITANTE);
            LOGGER.info("Placar de Pênaltis do Visitante: " + placarEstendidoEquipeVisitante);
            partidaDTO.setPlacarPenaltisVisitante(placarEstendidoEquipeVisitante);
        }
        catch (IOException e) {
            LOGGER.error(e.getMessage());
            return null;
        }

        return partidaDTO;
    }

    public StatusPartida getStatusPartida(final Document document) {
        StatusPartida statusPartida = StatusPartida.PARTIDA_NAO_INICIADA;

        if (!document.select(DIV_PARTIDA_NAO_INICIADA).isEmpty()) {
            statusPartida = StatusPartida.PARTIDA_NAO_INICIADA;
        }

        final Elements divJogoRolando = document.select(DIV_JOGO_ROLANDO);

        if (!divJogoRolando.isEmpty()) {
            final String tempoPartida = divJogoRolando.first().text();

            statusPartida = StatusPartida.PARTIDA_EM_ANDAMENTO;

            if (tempoPartida.contains(PENALTIS)) {
                statusPartida = StatusPartida.PARTIDA_PENALTIS;
            }
        }

        return !document.select(DIV_PARTIDA_ENCERRADA).isEmpty() ? StatusPartida.PARTIDA_ENCERRADA : statusPartida;
    }

    public String getTempoPartida(final Document document) {
        String tempoPartida = null;

        Elements divTempoPartida = document.select(DIV_PARTIDA_NAO_INICIADA);

        if (!divTempoPartida.isEmpty()) {
            tempoPartida = divTempoPartida.first().text();
        }

        divTempoPartida = document.select(DIV_JOGO_ROLANDO);

        if (!divTempoPartida.isEmpty()) {
            tempoPartida = divTempoPartida.first().text();
        }

        divTempoPartida = document.select(DIV_PARTIDA_ENCERRADA);

        if (!divTempoPartida.isEmpty()) {
            tempoPartida = divTempoPartida.first().text();
        }

        return formatarTempoPartida(tempoPartida);
    }

    public Integer getPlacarEquipe(final Document document, final String itemHtml) {
        return formatarPlacar(document.selectFirst(itemHtml).text());
    }

    public String getUrlLogoEquipe(final Document document, final String itemHtml) {
        final Element element = document.selectFirst(itemHtml);
        return element != null ? element.select(IMG_LOGO).attr(SRC) : null;
    }

    public String getNomeEquipe(final Document document, final String itemHtml) {
        final Element element = document.selectFirst(itemHtml);
        return element != null ? element.select(SPAN).text() : null;
    }

    public String getGolsEquipe(final Document document, final String itemHtml) {
        final List<String> golsEquipe = new ArrayList<>();

        final Elements gols = document.select(itemHtml).select(DIV_ITEM_GOLS);
        gols.forEach(e -> golsEquipe.add(e.select(DIV_ITEM_GOLS).text()));

        return golsEquipe.isEmpty() ? null : String.join(", ", golsEquipe);
    }

    public Integer getInfoPenalidades(final Document document, final String tipoEquipe) {
        if (!document.select(DIV_PENALIDADES).isEmpty()) {
            final String penalidades = document.select(DIV_PENALIDADES).text();
            final String completo    = penalidades.substring(0, 5).replace(" ", "");
            final String[] divisao   = completo.split("-");

            return formatarPlacar(tipoEquipe.equals(MANDANTE) ? divisao[0] : divisao[1]);
        }

        return null;
    }

    public String montarUrlPesquisa(final String nomeEquipeMandante, final String nomeEquipeVisitante, final String dataPartida) {
        try {
            final String equipeMandante = nomeEquipeMandante.replace(" ", "+").replace("-", "+");
            final String equipeVisitante = nomeEquipeVisitante.replace(" ", "+").replace("-", "+");
            return BASE_URL + equipeMandante + "+x+" + equipeVisitante + "+" + dataPartida + COMPLEMENTO_URL;
        }
        catch (final Exception e) {
            LOGGER.error("ERRO: {}", e.getMessage());
            return null;
        }
    }

    private static String formatarTempoPartida(final String tempo) {
        if (tempo.contains("'")) {
            return tempo.replace(" ", "").replace("'", "").concat(" min");
        }

        if (tempo.contains("+")) {
            return tempo.replace(" ", "").concat(" min");
        }

        return tempo;
    }

    private Integer formatarPlacar(final String placar) {
        try {
            return Integer.parseInt(placar);
        }
        catch (final Exception e) {
            return 0;
        }
    }
}