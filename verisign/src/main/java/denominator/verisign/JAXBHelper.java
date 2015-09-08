package denominator.verisign;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Singleton
class JAXBHelper {

	@Inject
	public JAXBHelper() {}

	private final LoadingCache<Class<?>, JAXBContext> jaxbContextCache = CacheBuilder.newBuilder()
			.build(
					new CacheLoader<Class<?>, JAXBContext>() {
						@Override
						public JAXBContext load(Class<?> clazz) throws Exception {
							return JAXBContext.newInstance(clazz);
						}
					}
			);

	public JAXBContext getJAXBContext(Class<?> clazz) throws JAXBException {
		try {
			return jaxbContextCache.get(clazz);
		} catch (ExecutionException e) {
			throw new JAXBException("Error when getting JAXBContext from cache", e);
		}
	}
	
	public JAXBContext getJAXBContext() {
		
		if(jaxbContextCache.asMap().values() != null) {
			Iterator<JAXBContext> iterator = jaxbContextCache.asMap().values().iterator();
			return iterator.hasNext() ? iterator.next() : null;
		}
		
		return null;
	}
	
}
