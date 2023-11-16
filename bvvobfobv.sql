SELECT  *
FROM BVV_Valgbare_verdier
inner join OBF_Objekt_Felt on BVV_Valgbare_verdier.BFO_Nr=OBF_Objekt_Felt.BFO_Nr and BVV_Valgbare_verdier.BVV_Type=OBF_Objekt_Felt.BVV_Type
INNER JOIN OBV_Verdi ON BVV_Valgbare_verdier.BVV_Kode=OBV_Verdi.OBV_Verdi_Tekst and OBF_Objekt_Felt.OBF_Nr=OBV_Verdi.OBF_Nr
where BVV_Valgbare_verdier.BFO_Nr=30

  