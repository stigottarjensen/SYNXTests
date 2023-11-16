SELECT  [BKB_Nr]
      ,[BVV_Valgbare_verdier].[BFO_Nr] as BFO_Nr
      ,[BBO_Nr]
      ,[BVV_Valgbare_verdier].BVV_Type as BVV_Type
      ,[BVV_Kode]
      ,rtrim([BVV_Tekst]) as bvvtekst
      ,[BVV_Verdi]
      ,[BVV_Gruppe]
      ,rtrim([OBF_Label]) as label
      ,[OBF_Type_Felt]
      ,[OBF_Sekvens]
      ,[OBF_Blankes]
      ,[OBF_Vise_i_Grid]
      ,[OBF_Vise_i_PDF]
      ,[OBT_Type]
      ,[OBJ_Nr]
      ,[OBV_Verdi_Tall]
      ,[OBV_Verdi_Tekst]
      ,[OBV_Verdi_Dato]
      ,[BKL_Nr]
FROM BVV_Valgbare_verdier
inner join OBF_Objekt_Felt on BVV_Valgbare_verdier.BFO_Nr=OBF_Objekt_Felt.BFO_Nr and BVV_Valgbare_verdier.BVV_Type=OBF_Objekt_Felt.BVV_Type
INNER JOIN OBV_Verdi ON BVV_Valgbare_verdier.BVV_Kode=OBV_Verdi.OBV_Verdi_Tekst and OBF_Objekt_Felt.OBF_Nr=OBV_Verdi.OBF_Nr
where BVV_Valgbare_verdier.BFO_Nr=30
order by BBO_Nr desc

  