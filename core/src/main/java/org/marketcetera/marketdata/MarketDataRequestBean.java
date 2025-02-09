package org.marketcetera.marketdata;

import static org.marketcetera.core.Util.ESCAPED_KEY_VALUE_DELIMITER;
import static org.marketcetera.core.Util.ESCAPED_KEY_VALUE_SEPARATOR;
import static org.marketcetera.core.Util.KEY_VALUE_DELIMITER;
import static org.marketcetera.core.Util.KEY_VALUE_SEPARATOR;
import static org.marketcetera.marketdata.MarketDataRequestBuilder.ASSETCLASS_KEY;
import static org.marketcetera.marketdata.MarketDataRequestBuilder.CONTENT_KEY;
import static org.marketcetera.marketdata.MarketDataRequestBuilder.EXCHANGE_KEY;
import static org.marketcetera.marketdata.MarketDataRequestBuilder.PARAMETERS_KEY;
import static org.marketcetera.marketdata.MarketDataRequestBuilder.PROVIDER_KEY;
import static org.marketcetera.marketdata.MarketDataRequestBuilder.SYMBOLS_KEY;
import static org.marketcetera.marketdata.MarketDataRequestBuilder.SYMBOL_DELIMITER;
import static org.marketcetera.marketdata.MarketDataRequestBuilder.UNDERLYINGSYMBOLS_KEY;
import static org.marketcetera.marketdata.MarketDataRequestBuilder.REQUEST_ID_KEY;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.concurrent.ThreadSafe;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.marketcetera.util.misc.ClassVersion;

/* $License$ */

/**
 * Contains the data necessary to compose a {@link MarketDataRequest}.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since 2.1.0
 */
@ThreadSafe
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="marketDataRequestBean")
@ClassVersion("$Id$")
public final class MarketDataRequestBean
        implements Serializable, Cloneable
{
    /**
     * Get the provider value.
     *
     * @return a <code>String</code> value
     */
    public String getProvider()
    {
        return provider;
    }
    /**
     * Sets the provider value.
     *
     * @param inProvider a <code>String</code> value
     */
    public void setProvider(String inProvider)
    {
        provider = inProvider;
    }
    /**
     * Get the exchange value.
     *
     * @return a <code>String</code> value
     */
    public String getExchange()
    {
        return exchange;
    }
    /**
     * Sets the exchange value.
     *
     * @param inExchange a <code>String</code> value
     */
    public void setExchange(String inExchange)
    {
        exchange = inExchange;
    }
    /**
     * Get the assetClass value.
     *
     * @return an <code>AssetClass</code> value
     */
    public AssetClass getAssetClass()
    {
        return assetClass;
    }
    /**
     * Sets the assetClass value.
     *
     * @param inAssetClass a <code>AssetClass</code> value
     */
    public void setAssetClass(AssetClass inAssetClass)
    {
        assetClass = inAssetClass;
    }
    /**
     * Get the symbols value.
     *
     * @return a <code>Set<String></code> value
     */
    public Set<String> getSymbols()
    {
        synchronized(symbols) {
            return Collections.unmodifiableSet(symbols);
        }
    }
    /**
     * Sets the symbols value.
     * 
     * <p>The given values replace the existing values.  Duplicate values
     * are discarded.  The values will be retained in their given order.
     * Null and empty values are allowed.
     *
     * @param inSymbols a <code>Collection&lt;String&gt;</code> value
     */
    public void setSymbols(Collection<String> inSymbols)
    {
        synchronized(symbols) {
            symbols.clear();
            symbols.addAll(inSymbols);
        }
    }
    /**
     * Get the underlyingSymbols value.
     *
     * @return a <code>Set<String></code> value
     */
    public Set<String> getUnderlyingSymbols()
    {
        synchronized(underlyingSymbols) {
            return Collections.unmodifiableSet(underlyingSymbols);
        }
    }
    /**
     * Sets the underlying symbols value.
     * 
     * <p>The given values replace the existing values.  Duplicate values
     * are discarded.  The values will be retained in their given order.
     * Null and empty values are allowed.
     *
     * @param inUnderlyingSymbols a <code>Collection&lt;String&gt;</code> value
     */
    public void setUnderlyingSymbols(Collection<String> inUnderlyingSymbols)
    {
        synchronized(underlyingSymbols) {
            underlyingSymbols.clear();
            underlyingSymbols.addAll(inUnderlyingSymbols);
        }
    }
    /**
     * Sets the parameter specified by the given key to the given value. 
     *
     * @param inKey a <code>String</code> value or <code>null</code>
     * @param inValue a <code>String</code> value or <code>null</code>
     */
    public void setParameter(String inKey,
                             String inValue)
    {
        synchronized(parameters) {
            parameters.put(inKey,
                           inValue);
        }
    }
    /**
     * Sets the parameters value.
     *
     * <p>The given values replace the existing values.  Duplicate values
     * are discarded.  The values will be retained in their given order.
     * 
     * @param inParameters a <code>Map&lt;String,String&gt;</code> value
     */
    public void setParameters(Map<String,String> inParameters)
    {
        synchronized(parameters) {
            parameters.clear();
            parameters.putAll(inParameters);
        }
    }
    /**
     * Get the parameters value.
     *
     * @return a <code>Map<String,String></code> value
     */
    public Map<String,String> getParameters()
    {
        synchronized(parameters) {
            return Collections.unmodifiableMap(parameters);
        }
    }
    /**
     * Get the content value.
     *
     * @return a <code>Set<Content></code> value
     */
    public Set<Content> getContent()
    {
        synchronized(content) {
            return Collections.unmodifiableSet(content);
        }
    }
    /**
     * Sets the contents value.
     * 
     * <p>The given values replace the existing values.  Duplicate values
     * are discarded.  The values will be retained in their given order.
     * Null values are allowed.
     *
     * @param inContent a <code>Collection&lt;Content&gt;</code> value
     */
    public void setContent(Collection<Content> inContent)
    {
        synchronized(content) {
            content.clear();
            content.addAll(inContent);
        }
    }
    /**
     * Sets the requestId value.
     *
     * @param inRequestId a <code>String</code> value
     */
    public void setRequestId(String inRequestId)
    {
        requestId = inRequestId;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public MarketDataRequestBean clone()
            throws CloneNotSupportedException
    {
        // intentionally not calling super.clone in order to establish different collections
        MarketDataRequestBean newBean = new MarketDataRequestBean();
        // simple attributes
        newBean.setAssetClass(assetClass);
        newBean.setExchange(exchange);
        newBean.setProvider(provider);
        newBean.setContent(content);
        newBean.setParameters(parameters);
        newBean.setSymbols(symbols);
        newBean.setUnderlyingSymbols(underlyingSymbols);
        newBean.setRequestId(requestId);
        return newBean;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17,
                                   53).append(assetClass)
                                      .append(requestId)
                                      .append(exchange)
                                      .append(provider)
                                      .append(content)
                                      .append(parameters)
                                      .append(symbols)
                                      .append(underlyingSymbols).toHashCode();
    }
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if(obj == null) { 
            return false;
        }
        if(obj == this) { 
            return true; 
        }
        if(!(obj instanceof MarketDataRequestBean)) {
          return false;
        }
        MarketDataRequestBean rhs = (MarketDataRequestBean)obj;
        return new EqualsBuilder().append(assetClass,
                                          rhs.assetClass)
                                  .append(requestId,
                                          rhs.requestId)
                                  .append(exchange,
                                          rhs.exchange)
                                  .append(provider,
                                          rhs.provider)
                                  .append(content,
                                          rhs.content)
                                  .append(parameters,
                                          rhs.parameters)
                                  .append(symbols,
                                          rhs.symbols)
                                  .append(underlyingSymbols,
                                          rhs.underlyingSymbols).isEquals();
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder output = new StringBuilder();
        boolean delimiterNeeded = false;
        if(requestId != null) {
            if(delimiterNeeded) {
                output.append(KEY_VALUE_DELIMITER);
            }
            output.append(REQUEST_ID_KEY).append(KEY_VALUE_SEPARATOR).append(requestId);
            delimiterNeeded = true;
        }
        Set<String> symbols = getSymbols();
        if(symbols != null && !symbols.isEmpty()) {
            if(delimiterNeeded) {
                output.append(KEY_VALUE_DELIMITER);
            }
            boolean symbolListDelimiterNeeded = false;
            output.append(SYMBOLS_KEY).append(KEY_VALUE_SEPARATOR);
            for(String symbol : symbols) {
                if(symbolListDelimiterNeeded) {
                    output.append(SYMBOL_DELIMITER);
                }
                output.append(symbol.trim());
                symbolListDelimiterNeeded = true;
            }
            delimiterNeeded = true;
        }
        Set<String> underlyingSymbols = getUnderlyingSymbols();
        if(underlyingSymbols != null && !underlyingSymbols.isEmpty()) {
            if(delimiterNeeded) {
                output.append(KEY_VALUE_DELIMITER);
            }
            boolean symbolListDelimiterNeeded = false;
            output.append(UNDERLYINGSYMBOLS_KEY).append(KEY_VALUE_SEPARATOR);
            for(String underlyingSymbol : underlyingSymbols) {
                if(symbolListDelimiterNeeded) {
                    output.append(SYMBOL_DELIMITER);
                }
                output.append(underlyingSymbol.trim());
                symbolListDelimiterNeeded = true;
            }
            delimiterNeeded = true;
        }
        String provider = getProvider();
        if(provider != null) {
            if(delimiterNeeded) {
                output.append(KEY_VALUE_DELIMITER);
            }
            output.append(PROVIDER_KEY).append(KEY_VALUE_SEPARATOR).append(String.valueOf(provider));
            delimiterNeeded = true;
        }
        Set<Content> content = getContent();
        if(!content.isEmpty()) {
            if(delimiterNeeded) {
                output.append(KEY_VALUE_DELIMITER);
            }
            output.append(CONTENT_KEY).append(KEY_VALUE_SEPARATOR).append(content.toString().replaceAll("[\\[\\] ]", //$NON-NLS-1$
                                                                                                        "")); //$NON-NLS-1$
            delimiterNeeded = true;
        }
        String exchange = getExchange();
        if(exchange != null) {
            if(delimiterNeeded) {
                output.append(KEY_VALUE_DELIMITER);
            }
            output.append(EXCHANGE_KEY).append(KEY_VALUE_SEPARATOR).append(String.valueOf(exchange));
            delimiterNeeded = true;
        }
        AssetClass assetClass = getAssetClass();
        if(assetClass != null) {
            if(delimiterNeeded) {
                output.append(KEY_VALUE_DELIMITER);
            }
            output.append(ASSETCLASS_KEY).append(KEY_VALUE_SEPARATOR).append(String.valueOf(assetClass));
            delimiterNeeded = true;
        }
        Map<String,String> parameters = getParameters();
        if(!parameters.isEmpty()) {
            if(delimiterNeeded) {
                output.append(KEY_VALUE_DELIMITER);
            }
            output.append(PARAMETERS_KEY).append(KEY_VALUE_SEPARATOR);
            boolean parameterDelimiterNeeded = false;
            for(Map.Entry<String,String> entry : parameters.entrySet()) {
                if(parameterDelimiterNeeded) {
                    output.append(ESCAPED_KEY_VALUE_DELIMITER);
                }
                output.append(entry.getKey()).append(ESCAPED_KEY_VALUE_SEPARATOR).append(entry.getValue());
                parameterDelimiterNeeded = true;
            }
            delimiterNeeded = true;
        }
        return output.toString();
    }
    /**
     * Get the request id value.
     *
     * @return a <code>String</code> value
     */
    public String getRequestId()
    {
        return requestId;
    }
    /**
     * the symbols for which to request data
     */
    @XmlElementWrapper(name="symbolList")
    private final Set<String> symbols = new LinkedHashSet<String>();
    /**
     * the underlying symbols for which to request data
     */
    @XmlElementWrapper(name="underlyingSymbolList")
    private final Set<String> underlyingSymbols = new LinkedHashSet<String>();
    /**
     * the map of custom request parameters 
     */
    @XmlElementWrapper(name="parameterist")
    private final Map<String,String> parameters = new HashMap<String, String>();
    /**
     * the request content
     */
    @XmlElementWrapper(name="contentList")
    private final Set<Content> content = new LinkedHashSet<Content>();
    /**
     * the provider key from which to request data
     */
    @XmlAttribute
    private volatile String provider;
    /**
     * the exchange from which to request data
     */
    @XmlAttribute
    private volatile String exchange;
    /**
     * the asset class
     */
    @XmlAttribute
    private volatile AssetClass assetClass;
    /**
     * request id value
     */
    @XmlAttribute
    private volatile String requestId = UUID.randomUUID().toString();
    private static final long serialVersionUID = -3197842100588340789L;
}
