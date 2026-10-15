3 benefits of hashing :
	securing passwords
	insuring correct data access(Data Integrity)
	storing large amount of data and accessing it in instant time 
Hashing equation :
	"Hello World" -> Hash Function(BCrypt, SHA256...) -> Hash Value(example : uwbg783h58g0hrnf0283hf)
Hashing features :
	Collision Resistance(never same output for 2 different inputs) : one method used to insure so is chaining (linked lists)
	Avalanche Effect(one change = completely different result)
	Deterministic(always same result)
	Irreversible
Calculating Hash :
	ASCII Value(each character have equivalent number) X Hash function 
Equivalent of Dictionary in Java : HashMap :
	Hash Table = older version 
	each element in a Hash Table or HashMap can be is stored in key:  value, Duplication(Hash Collision) is avoided using Chaining so each element would be linked lists of key : value pairs and using Exact Match with keys.
	Time Complexity :
		using array : O(n)
		using HashMap:
			best case(most common  in small data) : O(1)
			Worst Case :  O(1)+m
	HashMap vs Hash Table :
		HashMap allow one null key and multiple null values and also it's part of Java Collections Framework, it was introduced in Java 2.
		HashMap's are generally recommended .