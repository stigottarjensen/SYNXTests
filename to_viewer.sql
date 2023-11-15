SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE VIEW [dbo].[view_eiendom_oslo] AS
SELECT bbo.BBO_Nr as eiendom_nr ,bbo.BBO_Navn as eiendom_navn, bbo.BBO_InternNr as eiendom_intern_nr
, bbo.BPA_Postnr as post_nr, bpa.BPA_Poststed as post_sted
FROM BBO_Brannobjekt bbo
left join BPA_Postadresse bpa on bpa.BPA_Postnr = bbo.BPA_Postnr
where bbo.BFO_Nr = 30

GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE VIEW [dbo].[view_person_oslo] AS
SELECT bpe.BPE_Nr as person_nr ,rtrim(bpe.BPE_Fornavn) AS person_fornavn, rtrim(bpe.BPE_Etternavn) AS person_etternavn,
rtrim(bpe.BPE_Tittel) AS person_tittel,rtrim(bro.BRO_Nr) AS person_rolle, rtrim(bro.BRO_Rolle_tekst) AS rolle_tekst,
rtrim(bfi.BFI_Navn) AS virksomhet
FROM BPE_Person bpe
left join BRO_Rolle bro on bro.BRO_Nr = bpe.BRO_Nr
left join BFI_Firma bfi on bfi.BFI_Nr = bpe.BFI_Nr
where bpe.BFO_Nr = 30

GO