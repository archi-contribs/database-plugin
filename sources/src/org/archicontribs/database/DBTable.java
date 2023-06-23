package org.archicontribs.database;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * 
 * @author Herve Jouin
 *
 */
@AllArgsConstructor()
@Getter
@Setter
public class DBTable {
	String schema;
	@NonNull String name;
	@NonNull List<DBColumn> columns;
	List<String> primaryKeys;
	
	/**
	 * @return
	 */
	public String getFullName() {
		StringBuilder fullName = new StringBuilder();
		if ( !DBPlugin.isEmpty(this.schema) ) {
			fullName.append(this.schema);
			fullName.append(".");
		}
		fullName.append(this.name);
		
		return fullName.toString();
	}
	
	/**
	 * @return
	 */
	public String generateCreateStatement() {
		StringBuilder tbls = new StringBuilder();
		StringBuilder pkey = new StringBuilder();
		
		for ( int i = 0 ; i < this.columns.size() ; ++i ) {
			if ( tbls.length() != 0 )
				tbls.append(", ");
			DBColumn col = this.columns.get(i);
			tbls.append(col.getName());
			tbls.append(" ");
			tbls.append(col.getFullType());
		}
		
		if ( this.primaryKeys != null ) {
			for ( int i = 0 ; i < this.primaryKeys.size() ; ++i ) {
				if ( pkey.length() == 0 )
					pkey.append(", PRIMARY KEY (");
				else
					pkey.append(", ");
				String colPK = this.primaryKeys.get(i);
				pkey.append(colPK);
			}
			if ( pkey.length() != 0 )
				pkey.append(")");
		}
		
		StringBuilder createRequest = new StringBuilder("CREATE TABLE ");
		createRequest.append(getFullName());
		createRequest.append(" (");
		createRequest.append(tbls);
		createRequest.append(pkey);
		createRequest.append(")");
		
		return createRequest.toString();
	}
}
