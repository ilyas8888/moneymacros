from typing import List, Optional


def TwoSum(nums: Optional[List[int]], integer: int) -> int:
    for i in range(0, len(nums)-1):
        n = integer - nums[i]
        if n == nums[i + 1]:
            return [i, i + 1]
    return -1


def main():
    # Your core program logic goes here
    print("Hello from the main function!")
    try:
        print("Enter the nums array")
        array = list(map(int, input("Enter numbers separated by space: ").split()))
        targ = int(input("Enter the target number : "))
        print(TwoSum(array, targ))
    except ValueError:
        print("Enter correct types")


if __name__ == "__main__":
    main()
