DIST = less-concat.js
SRC = lib/less
less:
	@@echo "(function (require, exports) {" > ${DIST}
	@@cat build/ecma-5.js\
	      ${SRC}/parser.js\
	      ${SRC}/functions.js\
	      ${SRC}/tree/*.js\
	      ${SRC}/tree.js >> ${DIST}
	@@echo "})(require, exports);" >> ${DIST}
	@@echo ${DIST} built.
