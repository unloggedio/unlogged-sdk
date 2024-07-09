# CHANGELOG
## SDK v0.6.5
- The new release of SDK fixes bugs around complicated inheritance patterns from interfaces.
- Abstract methods were earlier duplicated and selectively logged. This created bugs, since the child class would not 
have the duplicate methods of parent class. This is now fixed.