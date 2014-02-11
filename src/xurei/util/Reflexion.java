package xurei.util;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Reflexion
{
	public <T extends Object, Y extends T> void copyFields(T from, Y too) 
	{
		Class<? extends Object> fromClass = from.getClass();
		Field[] fromFields = fromClass.getDeclaredFields();
		
		Class<? extends Object> tooClass = too.getClass();
		Field[] tooFields = tooClass.getDeclaredFields();
		
		if (fromFields != null && tooFields != null) 
		{
			for (Field tooF : tooFields) 
			{
				try 
				{
					// Check if that fields exists in the other method
					Field fromF = fromClass.getDeclaredField(tooF.getName());
					if (fromF.getType().equals(tooF.getType())) 
					{
						System.out.println("Setting "+tooF.getName());
						tooF.set(tooF, fromF);
					}
				} 
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Uses Java's reflection API to get access to an unaccessible field
	 * 
	 * @param typeOfClass
	 *					Class that the field should be read from
	 * @param typeOfField
	 *					The type of the field
	 * @return An Object of type Field
	 */
	public static StolenField stealField( Class typeOfClass, Class typeOfField )
	{
			Field[] fields = typeOfClass.getDeclaredFields();
			for( Field f : fields )
			{
					if( f.getType() == typeOfField )
					{
							try
							{
									f.setAccessible( true );
									return new StolenField(f, typeOfField);
							}
							catch (Exception e)
							{
									break; // Throw the Exception
							}
					}
			}
			throw new RuntimeException("WorldDownloader: Couldn't steal Field of type \"" + typeOfField + "\" from class \"" + typeOfClass + "\" !" );
	}
//----------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Uses Java's reflection API to get access to an unaccessible field
	 * @param typeOfClass Class that the field should be read from
	 * @param typeOfField The type of the field
	 * @return An Object of type Field
	 */
	public static Method stealMethod( Class typeOfClass, String methodName )
	{
		Method m;
		try
		{
			m = typeOfClass.getDeclaredMethod("getSaveVersion");
			m.setAccessible( true );
		} 
		catch (Exception e)
		{
			m = null;
		}
		if (m != null)
		{
			return m;
		}
		throw new RuntimeException("WorldDownloader: Couldn't steal Method \"" + methodName + "\" from class \"" + typeOfClass + "\" !" );
	}
//----------------------------------------------------------------------------------------------------------------------
	
	public static Object stealAndGetMethod(Object object, String methodName )
	{
		Class typeOfObject;
		
		if( object instanceof Class ) // User asked for static field:
		{
				typeOfObject = (Class) object;
				object = null;
		}
		else
				typeOfObject = object.getClass();
		
		Method m = stealMethod(typeOfObject, methodName);
		if (m != null)
		{
			try
			{
				return m.invoke(object);
			} 
			catch (Exception e)
			{
				throw new RuntimeException("WorldDownloader: Couldn't steal Method \"" + methodName + "\" from class \"" + typeOfObject + "\" !" );
			}
		}
		else
			return null;
	}
//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------

	public static class StolenField
	{
		Field f;
		Class typeOfField;
		
		public StolenField(Field _f, Class _typeOfField)
		{
			f = _f;
			typeOfField = _typeOfField;
		}
	//--------------------------------------------------------------------------------------------------------------------

		/**
		 * Uses Java's reflection API to get access to an unaccessible field
		 * @param object Object that the field should be read from or the type of the object if the field is static
		 * @param typeOfField The type of the field
		 * @return The value of the field
		 */
		public Object get(Object object)
		{
			Class typeOfObject = getObjectClass(object);
			
			try
			{
				return f.get( object );
			}
			catch( Exception e )
			{
				throw new RuntimeException("xurei.util.Reflexion: Couldn't get Field of type \"" + typeOfField + "\" from object \"" + object + "\" !" );
			}
		}
	//--------------------------------------------------------------------------------------------------------------------
		
		/**
		 * Uses Java's reflection API to set access to an unaccessible field
		 * @param object Object that the field should be read from or the type of the object if the field is static
		 * @param typeOfField The type of the field
		 * @return The value of the field
		 */
		public void set(Object object, Object value)
		{
			Class typeOfObject = getObjectClass(object);

			try
			{
				f.set(object, value);
			}
			catch( Exception e )
			{
				throw new RuntimeException("xurei.util.Reflexion: Couldn't set Field of type \"" + typeOfField + "\" from object \"" + object + "\" !" );
			}
		}
	//--------------------------------------------------------------------------------------------------------------------
		
		private Class getObjectClass(Object object)
		{
			if( object instanceof Class ) // User asked for static field:
					return (Class) object;
			else
				return object.getClass();
		}
	}
//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------
		
}
