SELECT trim(OBF_Label) as pivotfields, obf.OBT_Type, OBV_Nr, trim(BVV_Tekst) as bvvtekst
  FROM [Abisair].[dbo].[OBF_Objekt_Felt] as obf 
  left join OBV_Verdi as obv on obf.OBF_Nr=obv.OBF_Nr
   left join OBJ_Objekt obj on obv.OBJ_Nr=obj.OBJ_Nr
   left join BVV_Valgbare_verdier bvv on obf.BVV_Type=bvv.BVV_Type and obv.OBV_Verdi_Tekst=bvv.BVV_Kode and obj.BFO_Nr=bvv.BFO_Nr
   where OBF_Type_Felt='combobox'
